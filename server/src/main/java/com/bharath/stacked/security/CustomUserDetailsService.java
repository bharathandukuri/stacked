package com.bharath.stacked.security;

import com.bharath.stacked.model.User;
import com.bharath.stacked.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @NonNull
    public UserDetails loadUserByUsername(@NonNull String email) throws UsernameNotFoundException {
        String normalizedEmail = email.trim().toLowerCase();
        log.debug("Loading user details for authentication: {}", normalizedEmail);

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> {
                    log.warn("UserDetailsService: User not found with email: {}", normalizedEmail);
                    return new UsernameNotFoundException("User not found with email: " + normalizedEmail);
                });

        return new CustomUserDetails(user);
    }
}
