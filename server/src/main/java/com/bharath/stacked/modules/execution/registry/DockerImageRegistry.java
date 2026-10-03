package com.bharath.stacked.modules.execution.registry;

public enum DockerImageRegistry {

    ISOLATE_1_0(
            "execution/isolate",
            "1.0",
            "docker/isolate-1_0"
    ),

    JAVA_21(
            "execution/java",
            "21",
            "docker/java-21"
    );

    private final String name;
    private final String tag;
    private final String buildContext;

    DockerImageRegistry(
            String name,
            String tag,
            String buildContext
    ) {
        this.name = name;
        this.tag = tag;
        this.buildContext = buildContext;
    }

    public DockerImageDetails dockerImage() {
        return new DockerImageDetails(
                name,
                tag,
                buildContext
        );
    }
}