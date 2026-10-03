package com.bharath.stacked.modules.execution.registry;

public record DockerImageDetails(
        String name,
        String tag,
        String resourcePath
) {



    public String reference() {
        return name + ":" + tag;
    }
}