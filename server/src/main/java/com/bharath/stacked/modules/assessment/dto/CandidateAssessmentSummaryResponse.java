package com.bharath.stacked.modules.assessment.dto;

import com.bharath.stacked.modules.assessment.enums.AssessmentStatus;
import com.bharath.stacked.modules.assessment.enums.CandidateParticipationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CandidateAssessmentSummaryResponse {

    private String assessmentId;
    private String slug;
    private String title;
    private String description;
    private AssessmentStatus assessmentStatus;
    private Instant scheduledStart;
    private Instant scheduledEnd;
    private int durationMinutes;
    private Integer maxAttempts;
    private int attemptsUsed;
    private CandidateParticipationStatus assignmentStatus;
    private boolean canStart;
    private String cannotStartReason;
    private String activeAttemptId;
    private int totalQuestions;
    private double totalScore;
}

