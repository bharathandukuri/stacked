package com.bharath.stacked.modules.security.service.impl;

import com.bharath.stacked.common.api.PageResponse;
import com.bharath.stacked.exception.BadRequestException;
import com.bharath.stacked.exception.ConflictException;
import com.bharath.stacked.exception.ResourceNotFoundException;
import com.bharath.stacked.modules.security.dto.CreateUserRequest;
import com.bharath.stacked.modules.security.dto.UpdateUserRequest;
import com.bharath.stacked.modules.security.dto.UserFilterRequest;
import com.bharath.stacked.modules.security.dto.UserResponse;
import com.bharath.stacked.modules.security.model.Role;
import com.bharath.stacked.modules.security.model.User;
import com.bharath.stacked.modules.security.repository.UserRepository;
import com.bharath.stacked.modules.security.service.UserService;
import com.bharath.stacked.service.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final String SESSION_KEY_PREFIX = "stacked:auth:session:";
    private static final String USER_SESSIONS_PREFIX = "stacked:auth:user_sessions:";

    private final UserRepository userRepository;
    private final MongoTemplate mongoTemplate;
    private final PasswordEncoder passwordEncoder;
    private final RedisService redisService;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    @NonNull
    public PageResponse<UserResponse> listUsers(@Nullable UserFilterRequest filter) {
        if (filter == null) {
            filter = new UserFilterRequest();
        }

        Query query = new Query();
        List<Criteria> criteriaList = new ArrayList<>();

        if (filter.getRole() != null) {
            criteriaList.add(Criteria.where("role").is(filter.getRole()));
        }

        if (filter.getSearch() != null && !filter.getSearch().trim().isEmpty()) {
            String sanitizedSearch = Pattern.quote(filter.getSearch().trim());
            criteriaList.add(new Criteria().orOperator(
                    Criteria.where("name").regex(sanitizedSearch, "i"),
                    Criteria.where("email").regex(sanitizedSearch, "i")
            ));
        }

        if (!criteriaList.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criteriaList.toArray(new Criteria[0])));
        }

        long totalElements = mongoTemplate.count(query, User.class);

        int page = Math.max(0, filter.getPage());
        int size = filter.getSize() <= 0 ? 10 : Math.min(filter.getSize(), 100);

        String sortBy = filter.getSortBy();
        if (sortBy == null || sortBy.isBlank()) {
            sortBy = "createdAt";
        }
        Sort.Direction direction = "ASC".equalsIgnoreCase(filter.getSortDirection())
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        query.with(PageRequest.of(page, size, Sort.by(direction, sortBy)));

        List<User> users = mongoTemplate.find(query, User.class);
        List<UserResponse> items = users.stream()
                .map(UserResponse::fromUser)
                .toList();

        return PageResponse.of(items, page, size, totalElements);
    }

    @Override
    @NonNull
    public UserResponse getUserById(@NonNull String id) {
        if (id.isBlank()) {
            throw new BadRequestException("User ID cannot be blank.");
        }
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return UserResponse.fromUser(user);
    }

    @Override
    @NonNull
    public UserResponse createUser(@NonNull CreateUserRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new ConflictException("A user with email '" + request.email() + "' already exists.");
        }

        User user = User.builder()
                .name(request.name().trim())
                .email(normalizedEmail)
                .password(passwordEncoder.encode(request.password()))
                .role(request.role())
                .build();

        User savedUser = userRepository.save(user);
        log.info("Admin created new user [{}] with role [{}]", savedUser.getEmail(), savedUser.getRole());
        return UserResponse.fromUser(savedUser);
    }

    @Override
    @NonNull
    public UserResponse updateUser(@NonNull String id, @NonNull UpdateUserRequest request, @Nullable String currentUserId) {
        if (id.isBlank()) {
            throw new BadRequestException("User ID cannot be blank.");
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        // Protection: An admin cannot demote their own account
        boolean isSelf = isSelfUser(user, currentUserId);
        if (isSelf && user.getRole() == Role.ADMIN && request.role() != Role.ADMIN) {
            throw new BadRequestException("An admin cannot demote their own account.");
        }

        // Safety: Cannot demote the only remaining admin
        if (user.getRole() == Role.ADMIN && request.role() != Role.ADMIN && userRepository.countByRole(Role.ADMIN) <= 1) {
            throw new BadRequestException("Cannot demote the only remaining admin account.");
        }

        String normalizedEmail = request.email().trim().toLowerCase();
        if (!normalizedEmail.equalsIgnoreCase(user.getEmail())) {
            if (userRepository.existsByEmail(normalizedEmail)) {
                throw new ConflictException("A user with email '" + request.email() + "' already exists.");
            }
            user.setEmail(normalizedEmail);
        }

        user.setName(request.name().trim());
        user.setRole(request.role());

        if (request.password() != null && !request.password().isBlank()) {
            String newPassword = request.password().trim();
            if (newPassword.length() < 6 || newPassword.length() > 128 || newPassword.contains(" ")) {
                throw new BadRequestException("Password must be 6-128 characters long and contain no whitespace.");
            }
            user.setPassword(passwordEncoder.encode(newPassword));
            invalidateUserSessions(user.getId());
        }

        User savedUser = userRepository.save(user);
        log.info("Updated user [{}] (role: {})", savedUser.getEmail(), savedUser.getRole());
        return UserResponse.fromUser(savedUser);
    }

    @Override
    public void deleteUser(@NonNull String id, @Nullable String currentUserId) {
        if (id.isBlank()) {
            throw new BadRequestException("User ID cannot be blank.");
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        // Requirement: An admin cannot delete his own account
        if (isSelfUser(user, currentUserId)) {
            throw new BadRequestException("An admin cannot delete their own account.");
        }

        // Safety: Cannot delete the only remaining admin account
        if (user.getRole() == Role.ADMIN && userRepository.countByRole(Role.ADMIN) <= 1) {
            throw new BadRequestException("Cannot delete the only remaining admin account.");
        }

        userRepository.delete(user);
        invalidateUserSessions(user.getId());
        log.info("Admin [{}] deleted user [{}] ({})", currentUserId, user.getId(), user.getEmail());
    }

    private boolean isSelfUser(User user, @Nullable String currentPrincipal) {
        if (currentPrincipal == null || currentPrincipal.isBlank()) {
            return false;
        }
        return currentPrincipal.equals(user.getId()) || currentPrincipal.equalsIgnoreCase(user.getEmail());
    }

    private void invalidateUserSessions(@Nullable String userId) {
        if (userId == null || userId.isBlank()) {
            return;
        }
        try {
            String userSessionsKey = USER_SESSIONS_PREFIX + userId;
            Set<Object> tokenIds = redisTemplate.opsForSet().members(userSessionsKey);
            if (tokenIds != null) {
                for (Object tokenId : tokenIds) {
                    if (tokenId != null) {
                        redisService.delete(SESSION_KEY_PREFIX + userId + ":" + tokenId);
                    }
                }
            }
            redisService.delete(userSessionsKey);
        } catch (Exception e) {
            log.warn("Failed to clean up Redis sessions for user [{}]: {}", userId, e.getMessage());
        }
    }
}

