package com.bharath.stacked.modules.language.exception;

import com.bharath.stacked.exception.ResourceNotFoundException;

public class LanguageNotFoundException extends ResourceNotFoundException {

    public LanguageNotFoundException(String languageId) {
        super(String.format("Language not found with id: '%s'", languageId));
    }

    public LanguageNotFoundException(String message, Throwable cause) {
        super(message);
        initCause(cause);
    }
}
