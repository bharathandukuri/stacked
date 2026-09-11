package com.bharath.stacked.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidPasswordValidatorTest {

    private ValidPasswordValidator validator;

    @BeforeEach
    void setUp() {
        validator = new ValidPasswordValidator();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "admin123", // Simple lowercase + numbers (loose)
            "password", // Simple single-case (loose)
            "simple", // 6 chars minimum
            "Admin@123456", // Complex with special chars
            "Complex#Pass1",
            "secure_pass",
            "developer-2026",
            "K9mQ8vL2x"
    })
    @DisplayName("Loose but safe passwords are accepted")
    void validPasswords_ReturnTrue(String password) {
        assertTrue(validator.isValid(password, null));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "   ",
            "12345", // Less than 6 characters
            "pass with space", // Contains space
            "admin' OR '1'='1", // SQL injection with single quote
            "admin; DROP TABLE users;", // SQL command separator
            "admin$where", // NoSQL operator injection
            "admin--", // SQL line comment
            "admin/*comment*/", // SQL block comment
            "admin\" OR \"\"=\"\"", // SQL injection with double quote
            "admin\\escape", // Escape backslash
            "admin\0nullbyte", // Null byte
            "admin\r\ninjection" // CRLF
    })
    @DisplayName("Short, whitespace, or SQL/NoSQL injection passwords are rejected")
    void invalidPasswords_ReturnFalse(String password) {
        assertFalse(validator.isValid(password, null));
    }

    @Test
    @DisplayName("Null password returns false")
    void nullPassword_ReturnsFalse() {
        assertFalse(validator.isValid(null, null));
    }
}
