package com.bharath.stacked.modules.mcq.dto.request;

import com.bharath.stacked.modules.mcq.enums.McqStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateMcqStatusRequest(
        @NotNull(message = "Status is required")
        McqStatus status
) {}

