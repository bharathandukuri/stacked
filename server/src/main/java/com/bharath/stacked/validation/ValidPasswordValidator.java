package com.bharath.stacked.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.jspecify.annotations.Nullable;

public class ValidPasswordValidator implements ConstraintValidator<ValidPassword, String> {

    private static final int MIN_LENGTH = 6;
    private static final int MAX_LENGTH = 128;

    // Characters commonly used to escape strings, break quotes, concatenate
    // subqueries, or inject NoSQL operators
    private static final String DISALLOWED_INJECTION_CHARS = "'\";\\$\0";

    @Override
    public boolean isValid(@Nullable String password, @Nullable ConstraintValidatorContext context) {
        if (password == null) {
            return false;
        }

        if (password.length() < MIN_LENGTH || password.length() > MAX_LENGTH) {
            return false;
        }

        // Disallow SQL comment sequences ("--" and "/*", "*/")
        if (password.contains("--") || password.contains("/*") || password.contains("*/")) {
            return false;
        }

        for (int i = 0; i < password.length(); i++) {
            char c = password.charAt(i);

            // Disallow whitespace, null bytes, and non-printable control characters
            if (Character.isWhitespace(c) || c < 32 || c == 127) {
                return false;
            }

            // Disallow characters commonly exploited in SQL/NoSQL injection
            if (DISALLOWED_INJECTION_CHARS.indexOf(c) != -1) {
                return false;
            }
        }

        return true;
    }
}
