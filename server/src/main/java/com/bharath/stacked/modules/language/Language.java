package com.bharath.stacked.modules.language;

import com.bharath.stacked.modules.execution.dto.DockerImageDetails;
import com.bharath.stacked.modules.execution.registry.DockerImageRegistry;

import java.util.List;
import java.util.Map;

public interface Language {

    String id();

    String name();

    LanguageType type();

    String fileExtension();

    DockerImageRegistry dockerImage();

    default DockerImageDetails dockerImageDetails() {
        DockerImageRegistry registry = dockerImage();
        return registry != null ? registry.dockerImage() : null;
    }

    default String dockerImageReference() {
        DockerImageDetails details = dockerImageDetails();
        return details != null ? details.reference() : null;
    }

    List<String> run(List<String> fileNames);

    default List<String> run(List<String> fileNames, com.bharath.stacked.modules.execution.dto.CodeExecutionConstraints constraints) {
        return run(fileNames);
    }

    default List<String> run(String fileName) {
        return run(fileName != null ? List.of(fileName) : List.of());
    }

    default List<String> run(String fileName, com.bharath.stacked.modules.execution.dto.CodeExecutionConstraints constraints) {
        return run(fileName != null ? List.of(fileName) : List.of(), constraints);
    }

    default List<String> run(String... fileNames) {
        return run(fileNames != null ? List.of(fileNames) : List.of());
    }

    default String defaultFileName() {
        String ext = fileExtension() != null ? fileExtension() : "";
        return "solution" + ext;
    }

    default String resolveMainExecutable(Map<String, String> files) {
        if (files == null || files.isEmpty()) {
            return defaultFileName();
        }
        String defaultName = defaultFileName();
        if (files.containsKey(defaultName)) {
            return defaultName;
        }
        if (files.containsKey("solution" + fileExtension())) {
            return "solution" + fileExtension();
        }
        if (files.containsKey("Solution" + fileExtension())) {
            return "Solution" + fileExtension();
        }
        if (files.containsKey("Main" + fileExtension())) {
            return "Main" + fileExtension();
        }
        if (files.containsKey("main" + fileExtension())) {
            return "main" + fileExtension();
        }
        if (files.containsKey("index" + fileExtension())) {
            return "index" + fileExtension();
        }
        String ext = fileExtension();
        for (String file : files.keySet()) {
            if (ext != null && file.endsWith(ext)) {
                return file;
            }
        }
        return files.keySet().iterator().next();
    }

    default List<String> resolveSourceFiles(Map<String, String> files) {
        if (files == null || files.isEmpty()) {
            return List.of(defaultFileName());
        }
        String ext = fileExtension();
        List<String> matching = files.keySet().stream()
                .filter(name -> ext != null && name.endsWith(ext))
                .toList();
        if (!matching.isEmpty()) {
            return matching;
        }
        return List.copyOf(files.keySet());
    }
}

