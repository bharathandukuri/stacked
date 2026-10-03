package com.bharath.stacked.modules.language.exception;

import com.bharath.stacked.exception.BadRequestException;
import com.bharath.stacked.modules.language.LanguageType;

public class UnsupportedLanguageTypeException extends BadRequestException {

    public UnsupportedLanguageTypeException(String languageId, LanguageType expectedType, LanguageType actualType) {
        super(String.format("Language '%s' is of type %s, but expected type was %s.", languageId, actualType, expectedType));
    }

    public UnsupportedLanguageTypeException(String message) {
        super(message);
    }
}
