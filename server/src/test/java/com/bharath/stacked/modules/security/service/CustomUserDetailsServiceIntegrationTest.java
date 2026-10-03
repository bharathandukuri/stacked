package com.bharath.stacked.modules.security.service;

import com.bharath.stacked.AbstractIntegrationTest;
import com.bharath.stacked.modules.security.model.Role;
import com.bharath.stacked.modules.security.model.User;
import com.bharath.stacked.modules.security.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CustomUserDetailsService Live Integration Tests")
class CustomUserDetailsServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("loadUserByUsername returns valid UserDetails for persisted ADMIN in MongoDB")
    void loadUserByUsernameAdminLive() {
        User admin = User.builder()
                .name("Super Admin")
                .email("admin@stacked.com")
                .password(passwordEncoder.encode("Secret@123"))
                .role(Role.ADMIN)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        userRepository.save(admin);

        UserDetails userDetails = userDetailsService.loadUserByUsername("admin@stacked.com");

        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo("admin@stacked.com");
        assertThat(passwordEncoder.matches("Secret@123", userDetails.getPassword())).isTrue();
        assertThat(userDetails.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_ADMIN");
        assertThat(userDetails.isAccountNonExpired()).isTrue();
        assertThat(userDetails.isAccountNonLocked()).isTrue();
        assertThat(userDetails.isCredentialsNonExpired()).isTrue();
        assertThat(userDetails.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("loadUserByUsername normalizes uppercase email during lookup")
    void loadUserByUsernameCaseInsensitiveLive() {
        User user = User.builder()
                .name("Alice")
                .email("alice@stacked.com")
                .password(passwordEncoder.encode("Alice@123"))
                .role(Role.ADMIN)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        userRepository.save(user);

        UserDetails userDetails = userDetailsService.loadUserByUsername("ALICE@STACKED.COM");

        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo("alice@stacked.com");
        assertThat(userDetails.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_ADMIN");
    }

    @Test
    @DisplayName("loadUserByUsername throws UsernameNotFoundException when user does not exist in MongoDB")
    void loadUserByUsernameNotFoundLive() {
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("nonexistent@stacked.com"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("nonexistent@stacked.com");
    }
}
