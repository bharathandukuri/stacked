package com.bharath.stacked.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidEmailValidatorTest {

    private ValidEmailValidator validator;

    @BeforeEach
    void setUp() {
        validator = new ValidEmailValidator();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "admin@stacked.com",
            "john.doe@example.org",
            "support+test@domain.co.uk",
            "user_name@stacked-app.io"
    })
    @DisplayName("Valid standard email addresses return true")
    void validEmails_ReturnTrue(String email) {
        assertTrue(validator.isValid(email, null));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "   ",
            "invalid-email",
            "@missing-local.com",
            "missing-domain@",
            "user@.com",
            "user..double@domain.com",
            "user@domain..com",
            "user'OR'1'='1@domain.com", // SQL injection attempt
            "user$where@domain.com", // NoSQL operator injection
            "user<script>@domain.com", // XSS injection
            "user;drop table@domain.com",
            "user\0nullbyte@domain.com",
            "user\r\ninjection@domain.com"
    })
    @DisplayName("Invalid emails and injection strings return false")
    void invalidEmails_ReturnFalse(String email) {
        assertFalse(validator.isValid(email, null));
    }

    @Test
    @DisplayName("Null email returns false")
    void nullEmail_ReturnsFalse() {
        assertFalse(validator.isValid(null, null));
    }
}
