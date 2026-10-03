package com.bharath.stacked.modules.execution.registry;

import com.bharath.stacked.modules.execution.dto.DockerImageDetails;

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
    ),

    PYTHON_3_12(
            "execution/python",
            "3.12",
            "docker/python-3_12"
    ),

    C_17(
            "execution/c",
            "17",
            "docker/c-17"
    ),

    CPP_23(
            "execution/cpp",
            "23",
            "docker/cpp-23"
    ),

    JAVASCRIPT_NODE_20(
            "execution/javascript",
            "node-20",
            "docker/javascript-node-20"
    ),

    MYSQL_8_0(
            "execution/mysql",
            "8.0",
            "docker/mysql-8_0"
    ),

    POSTGRES_16(
            "execution/postgres",
            "16",
            "docker/postgres-16"
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