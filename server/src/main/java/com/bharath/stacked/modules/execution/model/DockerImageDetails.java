package com.bharath.stacked.modules.execution.model;

public record DockerImageDetails(
        String name,
        String tag,
        String resourcePath
) {



    public String reference() {
        return name + ":" + tag;
    }
}