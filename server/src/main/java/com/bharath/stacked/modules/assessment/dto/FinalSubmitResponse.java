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
public class FinalSubmitResponse {

    private String attemptId;
    private String assessmentId;
    private AttemptStatus status;
    private Instant submittedAt;
    private double score;
    private double maxScore;
    private Boolean passed;
    private String message;
}

