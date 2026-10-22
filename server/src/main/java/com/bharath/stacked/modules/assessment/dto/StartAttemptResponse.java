package com.bharath.stacked.modules.assessment.dto;

import com.bharath.stacked.modules.assessment.enums.AttemptStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StartAttemptResponse {

    private String attemptId;
    private String assessmentId;
    private int attemptNumber;
    private AttemptStatus status;
    private Instant startedAt;
    private Instant effectiveDeadline;
    private long remainingSeconds;
}

