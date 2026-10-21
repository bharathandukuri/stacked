package com.bharath.stacked.modules.security.service.impl;

import com.bharath.stacked.AbstractIntegrationTest;
import com.bharath.stacked.common.api.PageResponse;
import com.bharath.stacked.exception.BadRequestException;
import com.bharath.stacked.exception.ConflictException;
import com.bharath.stacked.modules.security.dto.CreateUserRequest;
import com.bharath.stacked.modules.security.dto.UpdateUserRequest;
import com.bharath.stacked.modules.security.dto.UserFilterRequest;
import com.bharath.stacked.modules.security.dto.UserResponse;
import com.bharath.stacked.modules.security.model.Role;
import com.bharath.stacked.modules.security.model.User;
import com.bharath.stacked.modules.security.repository.UserRepository;
import com.bharath.stacked.modules.security.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("UserService Live Integration Tests")
class UserServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User primaryAdmin;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        primaryAdmin = userRepository.save(User.builder()
                .name("Super Admin")
                .email("admin@stacked.com")
                .password(passwordEncoder.encode("AdminSecret123!"))
                .role(Role.ADMIN)
                .build());
    }

    @Test
    @DisplayName("Complete CRUD: Create user -> List -> Get -> Update -> Self-Delete Blocked -> Delete")
    void testCompleteUserCrudLifecycle() {
        // 1. Create a regular user
        CreateUserRequest createReq = new CreateUserRequest(
                "Bob Candidate",
                "bob@candidate.com",
                "Candidate123!",
                Role.USER
        );
        UserResponse createdUser = userService.createUser(createReq);

        assertThat(createdUser).isNotNull();
        assertThat(createdUser.id()).isNotBlank();
        assertThat(createdUser.name()).isEqualTo("Bob Candidate");
        assertThat(createdUser.email()).isEqualTo("bob@candidate.com");
        assertThat(createdUser.role()).isEqualTo(Role.USER);

        // Verify password in database is BCrypt hashed
        User dbUser = userRepository.findById(createdUser.id()).orElseThrow();
        assertThat(passwordEncoder.matches("Candidate123!", dbUser.getPassword())).isTrue();

        // 2. List users with filter
        UserFilterRequest filter = new UserFilterRequest();
        filter.setRole(Role.USER);
        PageResponse<UserResponse> listRes = userService.listUsers(filter);
        assertThat(listRes.getTotalElements()).isEqualTo(1L);
        assertThat(listRes.getItems().get(0).id()).isEqualTo(createdUser.id());

        // 3. Update user details and promote to ADMIN
        UpdateUserRequest updateReq = new UpdateUserRequest(
                "Bob Promoted",
                "bob.promoted@candidate.com",
                Role.ADMIN,
                "NewPass123!"
        );
        UserResponse updatedUser = userService.updateUser(createdUser.id(), updateReq, primaryAdmin.getId());
        assertThat(updatedUser.name()).isEqualTo("Bob Promoted");
        assertThat(updatedUser.email()).isEqualTo("bob.promoted@candidate.com");
        assertThat(updatedUser.role()).isEqualTo(Role.ADMIN);

        // 4. Test self-deletion block for admin
        assertThatThrownBy(() -> userService.deleteUser(primaryAdmin.getId(), primaryAdmin.getId()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("An admin cannot delete their own account.");

        assertThatThrownBy(() -> userService.deleteUser(primaryAdmin.getId(), primaryAdmin.getEmail()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("An admin cannot delete their own account.");

        // 5. Delete the created user by primary admin
        userService.deleteUser(createdUser.id(), primaryAdmin.getId());
        assertThat(userRepository.findById(createdUser.id())).isEmpty();

        // 6. Test cannot delete the last remaining admin
        assertThatThrownBy(() -> userService.deleteUser(primaryAdmin.getId(), "external-id"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Cannot delete the only remaining admin account.");
    }

    @Test
    @DisplayName("Validation: Duplicate email is rejected with ConflictException")
    void testDuplicateEmailRejected() {
        CreateUserRequest req1 = new CreateUserRequest("Admin Two", "admin@stacked.com", "Password123!", Role.ADMIN);

        assertThatThrownBy(() -> userService.createUser(req1))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already exists");
    }
}

