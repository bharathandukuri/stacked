package com.bharath.stacked.modules.coding_problem.controller;

import com.bharath.stacked.AbstractIntegrationTest;
import com.bharath.stacked.modules.security.model.Role;
import com.bharath.stacked.modules.security.util.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("CodingProblem Security and Authorization Integration Tests")
class CodingProblemSecurityIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private MockMvc mockMvc;
    private String adminToken;
    private String candidateToken;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        adminToken = jwtTokenProvider.generateAccessToken("admin-1", "admin@stacked.com", Role.ADMIN);
        candidateToken = jwtTokenProvider.generateAccessToken("user-1", "candidate@stacked.com", Role.USER);
    }

    @Test
    @DisplayName("GET /api/coding-problem/supported-languages is publicly accessible")
    void supportedLanguages_PubliclyAccessible_Returns200() throws Exception {
        mockMvc.perform(get("/api/coding-problem/supported-languages"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/coding-problem without authentication returns 401 Unauthorized")
    void listProblems_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/api/coding-problem"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/coding-problem with USER role returns 403 Forbidden")
    void listProblems_CandidateUser_Returns403() throws Exception {
        mockMvc.perform(get("/api/coding-problem")
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/coding-problem with ADMIN role returns 200 OK")
    void listProblems_Admin_Returns200() throws Exception {
        mockMvc.perform(get("/api/coding-problem")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/coding-problem without authentication returns 401 Unauthorized")
    void createProblem_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(post("/api/coding-problem")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/coding-problem with USER role returns 403 Forbidden")
    void createProblem_CandidateUser_Returns403() throws Exception {
        mockMvc.perform(post("/api/coding-problem")
                        .header("Authorization", "Bearer " + candidateToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/coding-problem/run-preview without authentication returns 401 Unauthorized")
    void runPreview_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(post("/api/coding-problem/run-preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/coding-problem/run-preview with USER role returns 403 Forbidden")
    void runPreview_CandidateUser_Returns403() throws Exception {
        mockMvc.perform(post("/api/coding-problem/run-preview")
                        .header("Authorization", "Bearer " + candidateToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/coding-problem/run-generator without authentication returns 401 Unauthorized")
    void runGenerator_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(post("/api/coding-problem/run-generator")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/coding-problem/run-generator with USER role returns 403 Forbidden")
    void runGenerator_CandidateUser_Returns403() throws Exception {
        mockMvc.perform(post("/api/coding-problem/run-generator")
                        .header("Authorization", "Bearer " + candidateToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE /api/coding-problem/{id} with USER role returns 403 Forbidden")
    void deleteProblem_CandidateUser_Returns403() throws Exception {
        mockMvc.perform(delete("/api/coding-problem/prob-1")
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PATCH /api/coding-problem/{id}/publish with USER role returns 403 Forbidden")
    void publishProblem_CandidateUser_Returns403() throws Exception {
        mockMvc.perform(patch("/api/coding-problem/prob-1/publish")
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isForbidden());
    }
}

