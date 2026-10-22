package com.bharath.stacked.modules.assessment.dto;

import com.bharath.stacked.modules.assessment.enums.CandidateParticipationStatus;
import com.bharath.stacked.modules.assessment.model.AssessmentAssignment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CandidateAssignmentReportResponse {

    private String id;
    private String assessmentId;
    private String candidateEmail;
    private String candidateUserId;
    private CandidateParticipationStatus status;
    private int attemptsCount;
    private String lastAttemptId;
    private Instant invitedAt;
    private Instant startedAt;
    private Instant completedAt;
    private Double latestScore;

    public static CandidateAssignmentReportResponse fromModel(AssessmentAssignment assignment, Double latestScore) {
        if (assignment == null) return null;
        return CandidateAssignmentReportResponse.builder()
                .id(assignment.getId())
                .assessmentId(assignment.getAssessmentId())
                .candidateEmail(assignment.getCandidateEmail())
                .candidateUserId(assignment.getCandidateUserId())
                .status(assignment.getStatus())
                .attemptsCount(assignment.getAttemptsCount())
                .lastAttemptId(assignment.getLastAttemptId())
                .invitedAt(assignment.getInvitedAt())
                .startedAt(assignment.getStartedAt())
                .completedAt(assignment.getCompletedAt())
                .latestScore(latestScore)
                .build();
    }
}

