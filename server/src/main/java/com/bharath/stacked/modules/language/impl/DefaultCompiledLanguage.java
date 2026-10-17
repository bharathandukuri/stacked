package com.bharath.stacked.modules.language.impl;

import com.bharath.stacked.modules.language.CompiledLanguage;
import com.bharath.stacked.modules.execution.registry.DockerImageRegistry;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

public record DefaultCompiledLanguage(
        String id,
        String name,
        String fileExtension,
        DockerImageRegistry dockerImage,
        Function<List<String>, List<String>> compileCommandGenerator,
        Function<List<String>, List<String>> runCommandGenerator,
        java.util.function.BiFunction<List<String>, com.bharath.stacked.modules.execution.dto.CodeExecutionConstraints, List<String>> runWithConstraintsGenerator,
        String defaultFileName,
        String buildArtifactName
) implements CompiledLanguage {

    public DefaultCompiledLanguage {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(fileExtension, "fileExtension must not be null");
        Objects.requireNonNull(dockerImage, "dockerImage must not be null");
        Objects.requireNonNull(compileCommandGenerator, "compileCommandGenerator must not be null");
        Objects.requireNonNull(runCommandGenerator, "runCommandGenerator must not be null");
        if (runWithConstraintsGenerator == null) {
            runWithConstraintsGenerator = (files, constraints) -> runCommandGenerator.apply(files);
        }
        defaultFileName = defaultFileName != null ? defaultFileName : "solution" + fileExtension;
        buildArtifactName = buildArtifactName != null ? buildArtifactName : "a.out";
    }

    public DefaultCompiledLanguage(
            String id,
            String name,
            String fileExtension,
            DockerImageRegistry dockerImage,
            Function<List<String>, List<String>> compileCommandGenerator,
            Function<List<String>, List<String>> runCommandGenerator,
            String defaultFileName,
            String buildArtifactName
    ) {
        this(id, name, fileExtension, dockerImage, compileCommandGenerator, runCommandGenerator, null,
                defaultFileName, buildArtifactName);
    }

    public DefaultCompiledLanguage(
            String id,
            String name,
            String fileExtension,
            DockerImageRegistry dockerImage,
            Function<List<String>, List<String>> compileCommandGenerator,
            Function<List<String>, List<String>> runCommandGenerator
    ) {
        this(id, name, fileExtension, dockerImage, compileCommandGenerator, runCommandGenerator, null,
                "solution" + fileExtension, "a.out");
    }

    @Override
    public List<String> compile(List<String> fileNames) {
        return compileCommandGenerator.apply(fileNames != null ? fileNames : List.of());
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

    @Override
    public String resolveMainExecutable(Map<String, String> files) {
        if ("a.out".equals(buildArtifactName)) {
            return "a.out";
        }
        return CompiledLanguage.super.resolveMainExecutable(files);
    }
}
