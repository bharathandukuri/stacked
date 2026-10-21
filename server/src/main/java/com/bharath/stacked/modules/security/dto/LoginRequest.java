package com.bharath.stacked.modules.security.dto;

import com.bharath.stacked.validation.ValidEmail;
import com.bharath.stacked.validation.ValidPassword;
import org.jspecify.annotations.NonNull;

public record LoginRequest(
        @ValidEmail(message = "Invalid email address.") @NonNull String email,
        @ValidPassword(message = "Password must be at least 6 characters and cannot contain spaces.") @NonNull String password) {
}

