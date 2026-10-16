package com.bharath.stacked.modules.language;

import java.util.List;

public interface DatabaseLanguage extends Language {

    @Override
    default LanguageType type() {
        return LanguageType.DATABASE;
    }

    default boolean requiresDaemonWait() {
        return true;
    }

    default List<String> readinessCheckCommand() {
        return List.of();
    }
}
