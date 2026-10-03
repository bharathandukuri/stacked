package com.bharath.stacked.modules.language;

import java.util.List;

public interface CompiledLanguage extends Language {

    @Override
    default LanguageType type() {
        return LanguageType.COMPILED;
    }

    List<String> compile(List<String> fileNames);

    default List<String> compile(String fileName) {
        return compile(fileName != null ? List.of(fileName) : List.of());
    }

    default List<String> compile(String... fileNames) {
        return compile(fileNames != null ? List.of(fileNames) : List.of());
    }
}
