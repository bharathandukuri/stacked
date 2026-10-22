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
import com.bharath.stacked.modules.security.service.TokenRevocationService;
import com.bharath.stacked.service.RedisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserServiceImpl Unit Tests")
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private MongoTemplate mongoTemplate;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RedisService redisService;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private TokenRevocationService tokenRevocationService;

    @Mock
    private SetOperations<String, Object> setOperations;

    @InjectMocks
    private UserServiceImpl userService;

    private User sampleAdmin;
    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleAdmin = User.builder()
                .id("admin-1")
                .name("Super Admin")
                .email("admin@stacked.com")
                .password("encoded_pass")
                .role(Role.ADMIN)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        sampleUser = User.builder()
                .id("user-1")
                .name("John Doe")
                .email("john@example.com")
                .password("encoded_pass")
                .role(Role.USER)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("listUsers: Returns paginated response with mapped items")
    void listUsers_Success() {
        UserFilterRequest filter = new UserFilterRequest();
        filter.setSearch("john");
        filter.setRole(Role.USER);
        filter.setPage(0);
        filter.setSize(10);

        when(mongoTemplate.count(any(Query.class), eq(User.class))).thenReturn(1L);
        when(mongoTemplate.find(any(Query.class), eq(User.class))).thenReturn(List.of(sampleUser));

        PageResponse<UserResponse> response = userService.listUsers(filter);

        assertThat(response).isNotNull();
        assertThat(response.getTotalElements()).isEqualTo(1L);
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).name()).isEqualTo("John Doe");
        assertThat(response.getItems().get(0).role()).isEqualTo(Role.USER);
    }

    @Test
    @DisplayName("getUserById: Returns UserResponse when user exists")
    void getUserById_Success() {
        when(userRepository.findById("user-1")).thenReturn(Optional.of(sampleUser));

        UserResponse response = userService.getUserById("user-1");

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo("user-1");
        assertThat(response.name()).isEqualTo("John Doe");
        assertThat(response.email()).isEqualTo("john@example.com");
        assertThat(response.role()).isEqualTo(Role.USER);
    }

    @Test
    @DisplayName("getUserById: Throws ResourceNotFoundException when user does not exist")
    void getUserById_NotFound() {
        when(userRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById("missing"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found with id: missing");
    }

    @Test
    @DisplayName("createUser: Successfully creates user with encoded password and role")
    void createUser_Success() {
        CreateUserRequest req = new CreateUserRequest("Jane Doe", "jane@example.com", "PlainPass123!", Role.USER);

        when(userRepository.existsByEmail("jane@example.com")).thenReturn(false);
        when(passwordEncoder.encode("PlainPass123!")).thenReturn("bcrypt_jane");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId("user-2");
            return saved;
        });

        UserResponse res = userService.createUser(req);

        assertThat(res).isNotNull();
        assertThat(res.id()).isEqualTo("user-2");
        assertThat(res.name()).isEqualTo("Jane Doe");
        assertThat(res.email()).isEqualTo("jane@example.com");
        assertThat(res.role()).isEqualTo(Role.USER);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User captured = captor.getValue();
        assertThat(captured.getPassword()).isEqualTo("bcrypt_jane");
        assertThat(captured.getRole()).isEqualTo(Role.USER);
    }

    @Test
    @DisplayName("createUser: Throws ConflictException when email is already registered")
    void createUser_DuplicateEmail() {
        CreateUserRequest req = new CreateUserRequest("Jane Doe", "john@example.com", "PlainPass123!", Role.USER);

        when(userRepository.existsByEmail("john@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(req))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already exists");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateUser: Successfully updates user name, email, and role")
    void updateUser_Success() {
        UpdateUserRequest req = new UpdateUserRequest("Johnathan Doe", "john.new@example.com", Role.ADMIN, null);

        when(userRepository.findById("user-1")).thenReturn(Optional.of(sampleUser));
        when(userRepository.existsByEmail("john.new@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse res = userService.updateUser("user-1", req, "admin-1");

        assertThat(res.name()).isEqualTo("Johnathan Doe");
        assertThat(res.email()).isEqualTo("john.new@example.com");
        assertThat(res.role()).isEqualTo(Role.ADMIN);
        verify(tokenRevocationService).revokeAllUserTokens("user-1");
    }

    @Test
    @DisplayName("updateUser: Updates password when provided and invalidates sessions")
    void updateUser_WithPassword() {
        UpdateUserRequest req = new UpdateUserRequest("John Doe", "john@example.com", Role.USER, "NewSecurePassword123!");

        when(userRepository.findById("user-1")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.encode("NewSecurePassword123!")).thenReturn("new_bcrypt");
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.members("auth:user_sessions:user-1")).thenReturn(Set.of("tok-1"));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse res = userService.updateUser("user-1", req, "admin-1");

        assertThat(res).isNotNull();
        verify(passwordEncoder).encode("NewSecurePassword123!");
        verify(tokenRevocationService).revokeAllUserTokens("user-1");
        verify(redisService).delete("auth:session:user-1:tok-1");
        verify(redisService).delete("auth:user_sessions:user-1");
    }

    @Test
    @DisplayName("updateUser: Throws BadRequestException when admin attempts to demote own account")
    void updateUser_SelfDemotionBlocked() {
        UpdateUserRequest req = new UpdateUserRequest("Super Admin", "admin@stacked.com", Role.USER, null);

        when(userRepository.findById("admin-1")).thenReturn(Optional.of(sampleAdmin));

        assertThatThrownBy(() -> userService.updateUser("admin-1", req, "admin-1"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("An admin cannot demote their own account.");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateUser: Throws BadRequestException when attempting to demote the only remaining admin")
    void updateUser_LastAdminDemotionBlocked() {
        UpdateUserRequest req = new UpdateUserRequest("Super Admin", "admin@stacked.com", Role.USER, null);

        when(userRepository.findById("admin-1")).thenReturn(Optional.of(sampleAdmin));
        when(userRepository.countByRole(Role.ADMIN)).thenReturn(1L);

        // Different admin attempting to demote admin-1
        assertThatThrownBy(() -> userService.updateUser("admin-1", req, "other-admin"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Cannot demote the only remaining admin account.");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("deleteUser: Successfully deletes user and cleans up Redis sessions")
    void deleteUser_Success() {
        when(userRepository.findById("user-1")).thenReturn(Optional.of(sampleUser));
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.members("auth:user_sessions:user-1")).thenReturn(Set.of("token-abc"));

        userService.deleteUser("user-1", "admin-1");

        verify(userRepository).delete(sampleUser);
        verify(tokenRevocationService).revokeAllUserTokens("user-1");
        verify(redisService).delete("auth:session:user-1:token-abc");
        verify(redisService).delete("auth:user_sessions:user-1");
    }

    @Test
    @DisplayName("deleteUser: Throws BadRequestException when admin attempts to delete own account by ID")
    void deleteUser_SelfDeletionBlocked_ById() {
        when(userRepository.findById("admin-1")).thenReturn(Optional.of(sampleAdmin));

        assertThatThrownBy(() -> userService.deleteUser("admin-1", "admin-1"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("An admin cannot delete their own account.");

        verify(userRepository, never()).delete(any());
    }

    @Test
    @DisplayName("deleteUser: Throws BadRequestException when admin attempts to delete own account by email match")
    void deleteUser_SelfDeletionBlocked_ByEmail() {
        when(userRepository.findById("admin-1")).thenReturn(Optional.of(sampleAdmin));

        assertThatThrownBy(() -> userService.deleteUser("admin-1", "admin@stacked.com"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("An admin cannot delete their own account.");

        verify(userRepository, never()).delete(any());
    }

    @Test
    @DisplayName("deleteUser: Throws BadRequestException when attempting to delete the only remaining admin")
    void deleteUser_LastAdminDeletionBlocked() {
        when(userRepository.findById("admin-1")).thenReturn(Optional.of(sampleAdmin));
        when(userRepository.countByRole(Role.ADMIN)).thenReturn(1L);

        assertThatThrownBy(() -> userService.deleteUser("admin-1", "super-root"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Cannot delete the only remaining admin account.");

        verify(userRepository, never()).delete(any());
    }
}

