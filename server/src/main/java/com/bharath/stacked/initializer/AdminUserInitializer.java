package com.bharath.stacked.initializer;

import com.bharath.stacked.model.Role;
import com.bharath.stacked.model.User;
import com.bharath.stacked.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminUserInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.default.email:admin@stacked.com}")
    private String defaultEmail;

    @Value("${admin.default.password:Admin@123456}")
    private String defaultPassword;

    @Value("${admin.default.name:Platform Admin}")
    private String defaultName;

    @Override
    public void run(ApplicationArguments args) {
        String normalizedEmail = defaultEmail.trim().toLowerCase();

        if (userRepository.findByEmail(normalizedEmail).isEmpty()) {
            log.info("No admin user found. Seeding initial default admin: {}", normalizedEmail);

            User admin = User.builder()
                    .name(defaultName)
                    .email(normalizedEmail)
                    .password(passwordEncoder.encode(defaultPassword))
                    .role(Role.ADMIN)
                    .build();

            userRepository.save(admin);
            log.info("Default admin user initialized successfully with email: {}", normalizedEmail);
        } else {
            log.debug("Admin user already exists for email: {}", normalizedEmail);
        }
    }
}
