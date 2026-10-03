package com.bharath.stacked.modules.security.service;

import com.bharath.stacked.modules.security.model.Role;
import com.bharath.stacked.modules.security.model.User;
import com.bharath.stacked.modules.security.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Collection;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomUserDetailsService and CustomUserDetails Unit Tests")
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService userDetailsService;

    @Test
    @DisplayName("loadUserByUsername normalizes email and returns CustomUserDetails when user exists")
    void loadUserByUsernameSuccess() {
        User user = User.builder()
                .id("u1")
                .name("Admin User")
                .email("admin@stacked.com")
                .password("hashed_password")
                .role(Role.ADMIN)
                .build();

        when(userRepository.findByEmail("admin@stacked.com")).thenReturn(Optional.of(user));

        UserDetails userDetails = userDetailsService.loadUserByUsername("  ADMIN@STACKED.COM  ");

        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo("admin@stacked.com");
        assertThat(userDetails.getPassword()).isEqualTo("hashed_password");
        assertThat(userDetails.isEnabled()).isTrue();
        assertThat(userDetails.isAccountNonExpired()).isTrue();
        assertThat(userDetails.isAccountNonLocked()).isTrue();
        assertThat(userDetails.isCredentialsNonExpired()).isTrue();

        Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();
        assertThat(authorities).extracting(GrantedAuthority::getAuthority).containsExactly("ROLE_ADMIN");

        CustomUserDetails customUserDetails = (CustomUserDetails) userDetails;
        assertThat(customUserDetails.getUser()).isSameAs(user);
        assertThat(customUserDetails.getId()).isEqualTo("u1");

        verify(userRepository).findByEmail("admin@stacked.com");
    }

    @Test
    @DisplayName("loadUserByUsername throws UsernameNotFoundException when user is not found")
    void loadUserByUsernameNotFound() {
        when(userRepository.findByEmail("unknown@stacked.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("unknown@stacked.com"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("User not found with email: unknown@stacked.com");
    }

    @Test
    @DisplayName("CustomUserDetails constructor throws NullPointerException if user is null")
    void customUserDetailsNullCheck() {
        assertThatThrownBy(() -> new CustomUserDetails(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("User cannot be null");
    }
}
