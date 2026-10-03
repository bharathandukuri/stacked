package com.bharath.stacked.modules.security.initializer;

import com.bharath.stacked.modules.security.model.Role;
import com.bharath.stacked.modules.security.model.User;
import com.bharath.stacked.modules.security.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

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

    @InjectMocks
    private AdminUserInitializer initializer;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(initializer, "defaultEmail", "  Admin@Stacked.com ");
        ReflectionTestUtils.setField(initializer, "defaultPassword", "AdminSecret123");
        ReflectionTestUtils.setField(initializer, "defaultName", "Super Admin");
    }

    @Test
    @DisplayName("Seeds default admin user when none exists")
    void seedsAdminWhenNoneExists() {
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
    @DisplayName("Skips seeding when admin user already exists")
    void skipsSeedingWhenAdminExists() {
        User existingUser = User.builder().email("admin@stacked.com").build();
        when(userRepository.findByEmail("admin@stacked.com")).thenReturn(Optional.of(existingUser));

        initializer.run(args);

        verify(userRepository, never()).save(any(User.class));
        verifyNoInteractions(passwordEncoder);
    }
}
