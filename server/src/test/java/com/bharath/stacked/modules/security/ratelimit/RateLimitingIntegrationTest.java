package com.bharath.stacked.modules.security.ratelimit;

import com.bharath.stacked.AbstractIntegrationTest;
import com.bharath.stacked.modules.security.model.Role;
import com.bharath.stacked.modules.security.model.User;
import com.bharath.stacked.modules.security.repository.UserRepository;
import com.bharath.stacked.modules.security.util.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Rate Limiting Live Integration Tests")
class RateLimitingIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        userRepository.deleteAll();

        // Clear any leftover rate limit keys in Redis
        Set<String> keys = stringRedisTemplate.keys("ratelimit:*");
        if (keys != null && !keys.isEmpty()) {
            stringRedisTemplate.delete(keys);
        }
    }

    @Test
    @DisplayName("Auth endpoint enforces limit of 10 requests per minute and returns 429 on 11th request")
    void authEndpoint_EnforcesRateLimit() throws Exception {
        String clientIp = "198.51.100.1";
        String loginPayload = "{\"email\":\"admin@stacked.com\",\"password\":\"wrong-pass\"}";

        // Send 10 requests within the limit
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(post("/api/auth/admin/login")
                    .with(request -> {
                        request.setRemoteAddr(clientIp);
                        return request;
                    })
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginPayload))
                    .andExpect(status().isUnauthorized())
                    .andExpect(header().string("X-RateLimit-Limit", "10"))
                    .andExpect(header().exists("X-RateLimit-Remaining"));
        }

        // 11th request must be rejected with 429 Too Many Requests
        mockMvc.perform(post("/api/auth/admin/login")
                .with(request -> {
                    request.setRemoteAddr(clientIp);
                    return request;
                })
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginPayload))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("X-RateLimit-Limit", "10"))
                .andExpect(header().string("X-RateLimit-Remaining", "0"))
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.errorCode").value("RATE_LIMIT_EXCEEDED"));
    }

    @Test
    @DisplayName("Different client IPs have independent auth rate limit quotas")
    void differentClientIps_IndependentQuotas() throws Exception {
        String ip1 = "198.51.100.10";
        String ip2 = "198.51.100.20";
        String loginPayload = "{\"email\":\"admin@stacked.com\",\"password\":\"wrong-pass\"}";

        // Exhaust IP 1 quota
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(post("/api/auth/admin/login")
                    .with(request -> {
                        request.setRemoteAddr(ip1);
                        return request;
                    })
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginPayload));
        }

        // IP 1 gets 429
        mockMvc.perform(post("/api/auth/admin/login")
                .with(request -> {
                    request.setRemoteAddr(ip1);
                    return request;
                })
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginPayload))
                .andExpect(status().isTooManyRequests());

        // IP 2 is unaffected and gets 401 (not 429)
        mockMvc.perform(post("/api/auth/admin/login")
                .with(request -> {
                    request.setRemoteAddr(ip2);
                    return request;
                })
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginPayload))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("X-RateLimit-Remaining", "9"));
    }

    @Test
    @DisplayName("Excluded endpoints such as /api/coding-problem/supported-languages are not rate-limited")
    void excludedEndpoints_NotRateLimited() throws Exception {
        mockMvc.perform(get("/api/coding-problem/supported-languages"))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("X-RateLimit-Limit"));
    }

    @Test
    @DisplayName("Authenticated user quota limits code execution endpoint")
    void authenticatedUser_ExecutionEndpointRateLimited() throws Exception {
        User admin = userRepository.save(User.builder()
                .name("Admin Exec")
                .email("adminexec@stacked.com")
                .password(passwordEncoder.encode("SecretPass123!"))
                .role(Role.ADMIN)
                .build());

        String token = jwtTokenProvider.generateAccessToken(admin.getId(), admin.getEmail(), admin.getRole());

        // Code execution limit is 15 requests per minute
        for (int i = 0; i < 15; i++) {
            mockMvc.perform(post("/api/admin/coding-problem/run-preview")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"problemType\":\"GENERIC\",\"language\":\"python-3.12\",\"solutionCode\":\"print(1)\"}"))
                    .andExpect(header().string("X-RateLimit-Limit", "15"));
        }

        // 16th request gets 429 Too Many Requests
        mockMvc.perform(post("/api/admin/coding-problem/run-preview")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"problemType\":\"GENERIC\",\"language\":\"python-3.12\",\"solutionCode\":\"print(1)\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("X-RateLimit-Remaining", "0"))
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.error.errorCode").value("RATE_LIMIT_EXCEEDED"));
    }
}
