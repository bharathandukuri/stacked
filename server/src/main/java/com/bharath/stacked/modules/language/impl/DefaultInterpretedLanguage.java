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
        Function<List<String>, List<String>> runCommandGenerator,
        java.util.function.BiFunction<List<String>, com.bharath.stacked.modules.execution.dto.CodeExecutionConstraints, List<String>> runWithConstraintsGenerator,
        String defaultFileName
) implements InterpretedLanguage {

    public DefaultInterpretedLanguage {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(fileExtension, "fileExtension must not be null");
        Objects.requireNonNull(dockerImage, "dockerImage must not be null");
        Objects.requireNonNull(runCommandGenerator, "runCommandGenerator must not be null");
        if (runWithConstraintsGenerator == null) {
            runWithConstraintsGenerator = (files, constraints) -> runCommandGenerator.apply(files);
        }
        defaultFileName = defaultFileName != null ? defaultFileName : "solution" + fileExtension;
    }

    public DefaultInterpretedLanguage(
            String id,
            String name,
            String fileExtension,
            DockerImageRegistry dockerImage,
            Function<List<String>, List<String>> runCommandGenerator,
            String defaultFileName
    ) {
        this(id, name, fileExtension, dockerImage, runCommandGenerator, null, defaultFileName);
    }

    public DefaultInterpretedLanguage(
            String id,
            String name,
            String fileExtension,
            DockerImageRegistry dockerImage,
            Function<List<String>, List<String>> runCommandGenerator
    ) {
        this(id, name, fileExtension, dockerImage, runCommandGenerator, null, "solution" + fileExtension);
    }

    @Override
    public List<String> run(List<String> fileNames) {
        return runCommandGenerator.apply(fileNames != null ? fileNames : List.of());
    }

    @Override
    public List<String> run(List<String> fileNames, com.bharath.stacked.modules.execution.dto.CodeExecutionConstraints constraints) {
        if (runWithConstraintsGenerator != null) {
            return runWithConstraintsGenerator.apply(fileNames != null ? fileNames : List.of(), constraints);
        }
        return run(fileNames);
    }
}
