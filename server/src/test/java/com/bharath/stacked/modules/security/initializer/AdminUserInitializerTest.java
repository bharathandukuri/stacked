package com.bharath.stacked.modules.security.initializer;

import com.bharath.stacked.modules.security.config.AdminProperties;
import com.bharath.stacked.modules.security.model.Role;
import com.bharath.stacked.modules.security.model.User;
import com.bharath.stacked.modules.security.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminUserInitializer Unit Tests")
class AdminUserInitializerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ApplicationArguments args;

    private AdminProperties adminProperties;
    private AdminUserInitializer initializer;

    @BeforeEach
    void setUp() {
        adminProperties = new AdminProperties(true, "  Admin@Stacked.com ", "AdminSecret123", "Super Admin");
        initializer = new AdminUserInitializer(userRepository, passwordEncoder, adminProperties);
    }

    @Test
    @DisplayName("Seeds default admin user when none exists")
    void seedsAdminWhenNoneExists() {
        when(userRepository.countByRole(Role.ADMIN)).thenReturn(0L);
        when(userRepository.findByEmail("admin@stacked.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("AdminSecret123")).thenReturn("encodedPassword");

        initializer.run(args);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());

        User savedUser = captor.getValue();
        assertThat(savedUser.getName()).isEqualTo("Super Admin");
        assertThat(savedUser.getEmail()).isEqualTo("admin@stacked.com");
        assertThat(savedUser.getPassword()).isEqualTo("encodedPassword");
        assertThat(savedUser.getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    @DisplayName("Skips seeding when an administrator already exists in the system")
    void skipsSeedingWhenAdminRoleAlreadyExists() {
        when(userRepository.countByRole(Role.ADMIN)).thenReturn(1L);

        initializer.run(args);

        verify(userRepository, never()).save(any(User.class));
        verify(userRepository, never()).findByEmail(anyString());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    @DisplayName("Skips seeding when email already belongs to an existing user")
    void skipsSeedingWhenEmailExists() {
        when(userRepository.countByRole(Role.ADMIN)).thenReturn(0L);
        User existingUser = User.builder().email("admin@stacked.com").role(Role.USER).build();
        when(userRepository.findByEmail("admin@stacked.com")).thenReturn(Optional.of(existingUser));

        initializer.run(args);

        verify(userRepository, never()).save(any(User.class));
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    @DisplayName("Skips seeding when admin provisioning is disabled")
    void skipsSeedingWhenDisabled() {
        adminProperties = new AdminProperties(false, "admin@stacked.com", "AdminSecret123", "Super Admin");
        initializer = new AdminUserInitializer(userRepository, passwordEncoder, adminProperties);

        initializer.run(args);

        verifyNoInteractions(userRepository, passwordEncoder);
    }

    @Test
    @DisplayName("Skips seeding when default credentials are blank or null")
    void skipsSeedingWhenCredentialsBlank() {
        adminProperties = new AdminProperties(true, "   ", "AdminSecret123", "Super Admin");
        initializer = new AdminUserInitializer(userRepository, passwordEncoder, adminProperties);

        initializer.run(args);

        verifyNoInteractions(userRepository, passwordEncoder);

        adminProperties = new AdminProperties(true, "admin@stacked.com", "", "Super Admin");
        initializer = new AdminUserInitializer(userRepository, passwordEncoder, adminProperties);

        initializer.run(args);

        verifyNoInteractions(userRepository, passwordEncoder);
    }

    @Test
    @DisplayName("Skips seeding when password is shorter than 8 characters")
    void skipsSeedingWhenPasswordTooShort() {
        adminProperties = new AdminProperties(true, "admin@stacked.com", "short", "Super Admin");
        initializer = new AdminUserInitializer(userRepository, passwordEncoder, adminProperties);

        when(userRepository.countByRole(Role.ADMIN)).thenReturn(0L);
        when(userRepository.findByEmail("admin@stacked.com")).thenReturn(Optional.empty());

        initializer.run(args);

        verify(userRepository, never()).save(any(User.class));
        verifyNoInteractions(passwordEncoder);
    }
}
