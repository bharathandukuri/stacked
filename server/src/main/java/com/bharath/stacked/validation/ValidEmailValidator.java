package com.bharath.stacked.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.jspecify.annotations.Nullable;

import java.util.regex.Pattern;

public class ValidEmailValidator implements ConstraintValidator<ValidEmail, String> {

    // RFC 5322 standard email pattern restricting to alphanumeric characters and standard email symbols
    private static final Pattern STRICT_EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,15}$"
    );

    // Explicit list of disallowed injection characters (SQL, NoSQL $ operators, XSS, CRLF, null bytes)
    private static final String DISALLOWED_CHARS = "$<>';\"{}\\\r\n\t|`\0";

    private static final int MAX_EMAIL_LENGTH = 254;

    @Override
    public boolean isValid(@Nullable String email, @Nullable ConstraintValidatorContext context) {
        if (email == null || email.isBlank()) {
            return false;
        }

        String trimmed = email.trim();

        if (trimmed.length() > MAX_EMAIL_LENGTH) {
            return false;
        }

        // Prevent injection and control characters
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if (c < 32 || DISALLOWED_CHARS.indexOf(c) != -1) {
                return false;
            }
        }

        // Prevent double dot traversal
        if (trimmed.contains("..")) {
            return false;
        }

        return STRICT_EMAIL_PATTERN.matcher(trimmed).matches();
    }
}
