package com.bharath.stacked.modules.language.impl;

import com.bharath.stacked.modules.language.InterpretedLanguage;
import com.bharath.stacked.modules.execution.registry.DockerImageRegistry;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public record DefaultInterpretedLanguage(
        String id,
        String name,
        String fileExtension,
        DockerImageRegistry dockerImage,
        Function<List<String>, List<String>> runCommandGenerator
) implements InterpretedLanguage {

    public DefaultInterpretedLanguage {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(fileExtension, "fileExtension must not be null");
        Objects.requireNonNull(dockerImage, "dockerImage must not be null");
        Objects.requireNonNull(runCommandGenerator, "runCommandGenerator must not be null");
    }

    @Override
    public List<String> run(List<String> fileNames) {
        return runCommandGenerator.apply(fileNames != null ? fileNames : List.of());
    }
}
