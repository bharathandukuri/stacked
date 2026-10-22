package com.bharath.stacked.modules.security;

import com.bharath.stacked.AbstractIntegrationTest;
import com.bharath.stacked.modules.security.dto.UpdateUserRequest;
import com.bharath.stacked.modules.security.model.Role;
import com.bharath.stacked.modules.security.model.User;
import com.bharath.stacked.modules.security.repository.UserRepository;
import com.bharath.stacked.modules.security.service.UserService;
import com.bharath.stacked.modules.security.util.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Token Revocation Live Integration Tests")
class TokenRevocationIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private UserService userService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Access token is immediately rejected after logout")
    void accessTokenRejectedImmediatelyAfterLogout() throws Exception {
        User user = userRepository.save(User.builder()
                .name("Alice Admin")
                .email("alice@stacked.com")
                .password(passwordEncoder.encode("SecretPass123!"))
                .role(Role.ADMIN)
                .build());

        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getEmail(), user.getRole());

        // 1. Verify access token works before logout
        mockMvc.perform(get("/api/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk());

        // 2. Call logout with the access token
        mockMvc.perform(post("/api/auth/logout")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk());

        // 3. Verify access token is immediately rejected after logout
        mockMvc.perform(get("/api/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Admin access token is immediately rejected after role change")
    void adminTokenRejectedImmediatelyAfterRoleChange() throws Exception {
        User admin1 = userRepository.save(User.builder()
                .name("Root Admin")
                .email("root@stacked.com")
                .password(passwordEncoder.encode("RootPass123!"))
                .role(Role.ADMIN)
                .build());

        User admin2 = userRepository.save(User.builder()
                .name("Secondary Admin")
                .email("secondary@stacked.com")
                .password(passwordEncoder.encode("SecPass123!"))
                .role(Role.ADMIN)
                .build());

        String admin2Token = jwtTokenProvider.generateAccessToken(admin2.getId(), admin2.getEmail(), admin2.getRole());

        // 1. Verify admin2 token can access admin endpoint before demotion
        mockMvc.perform(get("/api/admin/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + admin2Token))
                .andExpect(status().isOk());

        // Ensure small time tick so issuedAt is strictly before invalidation timestamp
        Thread.sleep(10);

        // 2. Demote admin2 to USER
        userService.updateUser(admin2.getId(), new UpdateUserRequest("Secondary Admin", "secondary@stacked.com", Role.USER, null), admin1.getId());

        // 3. Verify admin2's previous token is immediately revoked / rejected
        mockMvc.perform(get("/api/admin/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + admin2Token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Access token is immediately rejected after user deletion")
    void accessTokenRejectedImmediatelyAfterUserDeletion() throws Exception {
        User admin1 = userRepository.save(User.builder()
                .name("Primary Admin")
                .email("primary@stacked.com")
                .password(passwordEncoder.encode("PrimPass123!"))
                .role(Role.ADMIN)
                .build());

        User user = userRepository.save(User.builder()
                .name("Candidate User")
                .email("cand@stacked.com")
                .password(passwordEncoder.encode("CandPass123!"))
                .role(Role.USER)
                .build());

        String userToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getEmail(), user.getRole());

        // 1. Verify token works before deletion
        mockMvc.perform(get("/api/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isOk());

        Thread.sleep(10);

        // 2. Delete user
        userService.deleteUser(user.getId(), admin1.getId());

        // 3. Verify token is immediately rejected
        mockMvc.perform(get("/api/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isUnauthorized());
    }
}

