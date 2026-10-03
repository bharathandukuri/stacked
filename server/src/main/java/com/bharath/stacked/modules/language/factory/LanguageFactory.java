package com.bharath.stacked.modules.language.factory;

import com.bharath.stacked.modules.language.CompiledLanguage;
import com.bharath.stacked.modules.language.DatabaseLanguage;
import com.bharath.stacked.modules.language.InterpretedLanguage;
import com.bharath.stacked.modules.language.Language;
import com.bharath.stacked.modules.execution.registry.DockerImageRegistry;

import java.util.List;
import java.util.function.Function;

public interface LanguageFactory {

    Language create(String languageId);

    CompiledLanguage createCompiled(
            String id,
            String name,
            String fileExtension,
            DockerImageRegistry dockerImage,
            Function<List<String>, List<String>> compileCommandGenerator,
            Function<List<String>, List<String>> runCommandGenerator
    );

    InterpretedLanguage createInterpreted(
            String id,
            String name,
            String fileExtension,
            DockerImageRegistry dockerImage,
            Function<List<String>, List<String>> runCommandGenerator
    );

    DatabaseLanguage createDatabase(
            String id,
            String name,
            String fileExtension,
            DockerImageRegistry dockerImage,
            Function<List<String>, List<String>> runCommandGenerator
    );

    List<String> supportedLanguageIds();
}
