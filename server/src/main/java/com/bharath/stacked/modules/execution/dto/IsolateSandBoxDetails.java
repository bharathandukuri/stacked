package com.bharath.stacked.modules.execution.dto;

public record IsolateSandBoxDetails(
        int isolateBoxId,
        DockerContainerDetails dockerContainerDetails
) {
}
