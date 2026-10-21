package com.bharath.stacked.modules.security.controller;

import com.bharath.stacked.common.api.ApiResponse;
import com.bharath.stacked.common.api.PageResponse;
import com.bharath.stacked.modules.security.dto.CreateUserRequest;
import com.bharath.stacked.modules.security.dto.UpdateUserRequest;
import com.bharath.stacked.modules.security.dto.UserFilterRequest;
import com.bharath.stacked.modules.security.dto.UserResponse;
import com.bharath.stacked.modules.security.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserService userService;

    @GetMapping
    @NonNull
    public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> listUsers(
            @ModelAttribute UserFilterRequest filter) {
        PageResponse<UserResponse> response = userService.listUsers(filter);
        return ApiResponse.success(response, "Users retrieved successfully");
    }

    @GetMapping("/{id}")
    @NonNull
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(
            @PathVariable @NonNull String id) {
        UserResponse response = userService.getUserById(id);
        return ApiResponse.success(response, "User retrieved successfully");
    }

    @PostMapping
    @NonNull
    public ResponseEntity<ApiResponse<UserResponse>> createUser(
            @Valid @RequestBody @NonNull CreateUserRequest request) {
        UserResponse response = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "User created successfully").getBody());
    }

    @PutMapping("/{id}")
    @NonNull
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @PathVariable @NonNull String id,
            @Valid @RequestBody @NonNull UpdateUserRequest request,
            @Nullable Authentication authentication) {
        String currentUserId = authentication != null ? authentication.getName() : null;
        UserResponse response = userService.updateUser(id, request, currentUserId);
        return ApiResponse.success(response, "User updated successfully");
    }

    @DeleteMapping("/{id}")
    @NonNull
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @PathVariable @NonNull String id,
            @Nullable Authentication authentication) {
        String currentUserId = authentication != null ? authentication.getName() : null;
        userService.deleteUser(id, currentUserId);
        return ApiResponse.success(null, "User deleted successfully");
    }
}

