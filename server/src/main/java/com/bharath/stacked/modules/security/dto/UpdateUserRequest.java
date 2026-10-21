package com.bharath.stacked.modules.security.dto;

import com.bharath.stacked.modules.security.model.Role;
import com.bharath.stacked.validation.ValidEmail;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;

public record UpdateUserRequest(
        @NotBlank(message = "Name is required")
        @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
        String name,

        @NotBlank(message = "Email is required")
        @ValidEmail(message = "Invalid email address format or contains unsafe characters.")
        String email,

        @NotNull(message = "Role is required")
        Role role,

        @Nullable
        String password
) {}

