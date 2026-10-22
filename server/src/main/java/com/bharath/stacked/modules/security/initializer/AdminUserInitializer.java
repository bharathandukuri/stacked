package com.bharath.stacked.modules.security.initializer;

import com.bharath.stacked.modules.security.model.Role;
import com.bharath.stacked.modules.security.model.User;
import com.bharath.stacked.modules.security.repository.UserRepository;
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

    @Value("${admin.default.enabled:true}")
    private boolean enabled;

    @Value("${admin.default.email:}")
    private String defaultEmail;

    @Value("${admin.default.password:}")
    private String defaultPassword;

    @Value("${admin.default.name:Platform Admin}")
    private String defaultName;

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) {
            log.info("Default admin provisioning is disabled (admin.default.enabled=false).");
            return;
        }

        if (defaultEmail == null || defaultEmail.isBlank() || defaultPassword == null || defaultPassword.isBlank()) {
            log.info("Default admin provisioning credentials not configured. Skipping admin user seeding.");
            return;
        }

        String normalizedEmail = defaultEmail.trim().toLowerCase();

        if (userRepository.countByRole(Role.ADMIN) > 0 || userRepository.findByEmail(normalizedEmail).isPresent()) {
            log.debug("Admin user already exists in the system. Skipping default admin seeding.");
            return;
        }

        if (defaultPassword.length() < 8) {
            log.warn("Default admin password must be at least 8 characters long. Skipping admin user seeding.");
            return;
        }

        if ("Admin@123456".equals(defaultPassword)) {
            log.warn("SECURITY WARNING: Using known default admin password 'Admin@123456'. Change this in production!");
        }

        log.info("No admin user found. Seeding initial bootstrap admin: {}", normalizedEmail);

        User admin = User.builder()
                .name(defaultName != null && !defaultName.isBlank() ? defaultName.trim() : "Platform Admin")
                .email(normalizedEmail)
                .password(passwordEncoder.encode(defaultPassword))
                .role(Role.ADMIN)
                .build();

        userRepository.save(admin);
        log.info("Default admin user initialized successfully with email: {}", normalizedEmail);
    }
}
