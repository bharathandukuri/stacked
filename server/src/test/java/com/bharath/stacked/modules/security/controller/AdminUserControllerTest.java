package com.bharath.stacked.modules.security.controller;

import com.bharath.stacked.common.api.PageResponse;
import com.bharath.stacked.exception.BadRequestException;
import com.bharath.stacked.exception.ConflictException;
import com.bharath.stacked.exception.GlobalExceptionHandler;
import com.bharath.stacked.exception.ResourceNotFoundException;
import com.bharath.stacked.modules.security.dto.CreateUserRequest;
import com.bharath.stacked.modules.security.dto.UpdateUserRequest;
import com.bharath.stacked.modules.security.dto.UserResponse;
import com.bharath.stacked.modules.security.model.Role;
import com.bharath.stacked.modules.security.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminUserController Unit Tests")
class AdminUserControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private UserService userService;

    @InjectMocks
    private AdminUserController adminUserController;

    private UserResponse sampleUserResponse;
    private UsernamePasswordAuthenticationToken adminAuth;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminUserController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();

        sampleUserResponse = new UserResponse(
                "u-123",
                "Alice Admin",
                "alice@stacked.com",
                Role.ADMIN,
                Instant.now(),
                Instant.now()
        );

        adminAuth = new UsernamePasswordAuthenticationToken(
                "admin-principal-id",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
    }

    @Test
    @DisplayName("GET /api/admin/users: Returns 200 with PageResponse")
    void listUsers_Success() throws Exception {
        PageResponse<UserResponse> page = PageResponse.of(List.of(sampleUserResponse), 0, 10, 1);
        when(userService.listUsers(any())).thenReturn(page);

        mockMvc.perform(get("/api/admin/users")
                        .principal(adminAuth)
                        .param("search", "Alice")
                        .param("role", "ADMIN")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].id").value("u-123"))
                .andExpect(jsonPath("$.data.items[0].name").value("Alice Admin"))
                .andExpect(jsonPath("$.data.items[0].role").value("ADMIN"));
    }

    @Test
    @DisplayName("GET /api/admin/users/{id}: Returns 200 with UserResponse")
    void getUserById_Success() throws Exception {
        when(userService.getUserById("u-123")).thenReturn(sampleUserResponse);

        mockMvc.perform(get("/api/admin/users/u-123")
                        .principal(adminAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("u-123"))
                .andExpect(jsonPath("$.data.email").value("alice@stacked.com"));
    }

    @Test
    @DisplayName("GET /api/admin/users/{id}: Returns 404 when not found")
    void getUserById_NotFound() throws Exception {
        when(userService.getUserById("u-404")).thenThrow(new ResourceNotFoundException("User not found"));

        mockMvc.perform(get("/api/admin/users/u-404")
                        .principal(adminAuth))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("POST /api/admin/users: Returns 201 Created on valid request")
    void createUser_Success() throws Exception {
        CreateUserRequest req = new CreateUserRequest("Bob Candidate", "bob@example.com", "SecureP@ss1", Role.USER);
        UserResponse created = new UserResponse("u-456", "Bob Candidate", "bob@example.com", Role.USER, Instant.now(), Instant.now());

        when(userService.createUser(any(CreateUserRequest.class))).thenReturn(created);

        mockMvc.perform(post("/api/admin/users")
                        .principal(adminAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("u-456"))
                .andExpect(jsonPath("$.data.role").value("USER"));
    }

    @Test
    @DisplayName("POST /api/admin/users: Returns 400 when name is missing")
    void createUser_ValidationError() throws Exception {
        String invalidJson = """
                {
                  "name": "",
                  "email": "invalid-email",
                  "password": "123",
                  "role": "USER"
                }
                """;

        mockMvc.perform(post("/api/admin/users")
                        .principal(adminAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("POST /api/admin/users: Returns 409 when email already exists")
    void createUser_Conflict() throws Exception {
        CreateUserRequest req = new CreateUserRequest("Bob", "bob@example.com", "SecureP@ss1", Role.USER);
        when(userService.createUser(any(CreateUserRequest.class)))
                .thenThrow(new ConflictException("Email already exists"));

        mockMvc.perform(post("/api/admin/users")
                        .principal(adminAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("PUT /api/admin/users/{id}: Returns 200 on successful update")
    void updateUser_Success() throws Exception {
        UpdateUserRequest req = new UpdateUserRequest("Bob Updated", "bob@example.com", Role.USER, null);
        UserResponse updated = new UserResponse("u-456", "Bob Updated", "bob@example.com", Role.USER, Instant.now(), Instant.now());

        when(userService.updateUser(eq("u-456"), any(UpdateUserRequest.class), any()))
                .thenReturn(updated);

        mockMvc.perform(put("/api/admin/users/u-456")
                        .principal(adminAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Bob Updated"));
    }

    @Test
    @DisplayName("DELETE /api/admin/users/{id}: Returns 200 on successful deletion")
    void deleteUser_Success() throws Exception {
        doNothing().when(userService).deleteUser(eq("u-456"), any());

        mockMvc.perform(delete("/api/admin/users/u-456")
                        .principal(adminAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("DELETE /api/admin/users/{id}: Returns 400 when attempting self-deletion")
    void deleteUser_SelfDeletionBlocked() throws Exception {
        doThrow(new BadRequestException("An admin cannot delete their own account."))
                .when(userService).deleteUser(eq("admin-principal-id"), any());

        mockMvc.perform(delete("/api/admin/users/admin-principal-id")
                        .principal(adminAuth))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("An admin cannot delete their own account."));
    }
}
