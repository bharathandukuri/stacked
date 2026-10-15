package com.bharath.stacked.modules.execution.dto;
import java.time.Instant;

public record ManagedContainer(
        String containerId,
        String containerName,
        String imageReference,
        Instant createdAt
) {}
