package com.bharath.stacked.modules.language;

public interface DatabaseLanguage extends Language {

    @Override
    default LanguageType type() {
        return LanguageType.DATABASE;
    }
}
