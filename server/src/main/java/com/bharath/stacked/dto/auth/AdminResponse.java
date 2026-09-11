package com.bharath.stacked.dto.auth;

import com.bharath.stacked.model.Role;
import com.bharath.stacked.model.User;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.time.Instant;

public record AdminResponse(
        @Nullable String id,
        @NonNull String name,
        @NonNull String email,
        @NonNull Role role,
        @Nullable Instant createdAt,
        @Nullable Instant updatedAt) {
    @Nullable
    public static AdminResponse fromUser(@Nullable User user) {
        if (user == null) {
            return null;
        }
        return new AdminResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getCreatedAt(),
                user.getUpdatedAt());
    }
}
