package com.bharath.stacked.modules.execution.model;

public record SandBoxDetails(
        int isolateBoxId,
        DockerContainerDetails dockerContainerDetails
) {
}
