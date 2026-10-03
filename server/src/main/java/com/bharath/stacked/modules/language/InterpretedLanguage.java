package com.bharath.stacked.modules.language;

public interface InterpretedLanguage extends Language {

    @Override
    default LanguageType type() {
        return LanguageType.INTERPRETED;
    }
}
