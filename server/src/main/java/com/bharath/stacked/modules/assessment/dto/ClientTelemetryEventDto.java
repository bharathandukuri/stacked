package com.bharath.stacked.modules.assessment.dto;

import com.bharath.stacked.modules.assessment.enums.AssessmentAuditEventType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientTelemetryEventDto {

    @NotNull
    private AssessmentAuditEventType eventType;

    private String questionId;

    private Map<String, Object> metadata;

    private Instant clientTimestamp;
}

