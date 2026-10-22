package com.bharath.stacked.modules.assessment.dto;

import com.bharath.stacked.modules.assessment.enums.AttemptStatus;
import com.bharath.stacked.modules.assessment.model.AssessmentAttempt;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttemptReportResponse {

    private String id;
    private String assessmentId;
    private String candidateUserId;
    private String candidateEmail;
    private int attemptNumber;
    private AttemptStatus status;
    private Instant startedAt;
    private Instant effectiveDeadline;
    private Instant submittedAt;
    private Double score;
    private Double maxPossibleScore;
    private Boolean passed;
    private long integrityFlagsCount;

    public static AttemptReportResponse fromModel(AssessmentAttempt attempt, long integrityFlagsCount) {
        if (attempt == null) return null;
        return AttemptReportResponse.builder()
                .id(attempt.getId())
                .assessmentId(attempt.getAssessmentId())
                .candidateUserId(attempt.getCandidateUserId())
                .candidateEmail(attempt.getCandidateEmail())
                .attemptNumber(attempt.getAttemptNumber())
                .status(attempt.getStatus())
                .startedAt(attempt.getStartedAt())
                .effectiveDeadline(attempt.getEffectiveDeadline())
                .submittedAt(attempt.getSubmittedAt())
                .score(attempt.getScore())
                .maxPossibleScore(attempt.getMaxPossibleScore())
                .passed(attempt.getPassed())
                .integrityFlagsCount(integrityFlagsCount)
                .build();
    }
}

