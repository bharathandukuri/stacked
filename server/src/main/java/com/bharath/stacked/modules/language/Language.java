package com.bharath.stacked.modules.language;

import com.bharath.stacked.modules.execution.dto.DockerImageDetails;
import com.bharath.stacked.modules.execution.registry.DockerImageRegistry;

import java.util.List;

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

    default List<String> run(String fileName) {
        return run(fileName != null ? List.of(fileName) : List.of());
    }

    default List<String> run(String... fileNames) {
        return run(fileNames != null ? List.of(fileNames) : List.of());
    }
}
