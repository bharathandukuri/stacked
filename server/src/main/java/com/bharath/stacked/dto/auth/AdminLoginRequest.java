package com.bharath.stacked.dto.auth;

import com.bharath.stacked.validation.ValidEmail;
import com.bharath.stacked.validation.ValidPassword;
import org.jspecify.annotations.NonNull;

public record AdminLoginRequest(
        @ValidEmail(message = "Invalid email address.") @NonNull String email,

        @ValidPassword(message = "Password must be at least 6 characters and cannot contain spaces.") @NonNull String password) {
}
