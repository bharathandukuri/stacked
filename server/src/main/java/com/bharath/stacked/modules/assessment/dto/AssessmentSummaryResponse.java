package com.bharath.stacked.modules.assessment.dto;

import com.bharath.stacked.modules.assessment.enums.AssessmentSelectionMode;
import com.bharath.stacked.modules.assessment.enums.AssessmentStatus;
import com.bharath.stacked.modules.assessment.model.Assessment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssessmentSummaryResponse {

    private String id;
    private String slug;
    private String title;
    private String description;
    private AssessmentStatus status;
    private AssessmentSelectionMode selectionMode;
    private Instant scheduledStart;
    private Instant scheduledEnd;
    private int durationMinutes;
    private Integer maxAttempts;
    private Double passingScore;
    private double totalScore;
    private int totalQuestions;
    private int mcqCount;
    private int codingCount;
    private int assignedCandidateCount;
    private String invitationCode;
    private boolean invitationCodeEnabled;
    private Instant invitationCodeExpiresAt;
    private Integer invitationCodeMaxUses;
    private int invitationCodeUsedCount;
    private String authorId;
    private boolean manuallyActivated;
    private Instant manuallyActivatedAt;
    private boolean manuallyCompleted;
    private Instant manuallyCompletedAt;
    private String manualInterventionMessage;
    private Instant createdAt;
    private Instant updatedAt;

    public static AssessmentSummaryResponse fromModel(Assessment assessment) {
        if (assessment == null) return null;
        return AssessmentSummaryResponse.builder()
                .id(assessment.getId())
                .slug(assessment.getSlug())
                .title(assessment.getTitle())
                .description(assessment.getDescription())
                .status(assessment.getEffectiveStatus())
                .selectionMode(assessment.getSelectionMode())
                .scheduledStart(assessment.getScheduledStart())
                .scheduledEnd(assessment.getScheduledEnd())
                .durationMinutes(assessment.getDurationMinutes())
                .maxAttempts(assessment.getMaxAttempts())
                .passingScore(assessment.getPassingScore())
                .totalScore(assessment.getTotalScore())
                .totalQuestions(assessment.getTotalQuestions())
                .mcqCount(assessment.getMcqCount())
                .codingCount(assessment.getCodingCount())
                .assignedCandidateCount(assessment.getAssignedCandidateCount())
                .invitationCode(assessment.getInvitationCode())
                .invitationCodeEnabled(assessment.isInvitationCodeEnabled())
                .invitationCodeExpiresAt(assessment.getInvitationCodeExpiresAt())
                .invitationCodeMaxUses(assessment.getInvitationCodeMaxUses())
                .invitationCodeUsedCount(assessment.getInvitationCodeUsedCount())
                .authorId(assessment.getAuthorId())
                .manuallyActivated(assessment.isManuallyActivated())
                .manuallyActivatedAt(assessment.getManuallyActivatedAt())
                .manuallyCompleted(assessment.isManuallyCompleted())
                .manuallyCompletedAt(assessment.getManuallyCompletedAt())
                .manualInterventionMessage(assessment.getManualInterventionMessage())
                .createdAt(assessment.getCreatedAt())
                .updatedAt(assessment.getUpdatedAt())
                .build();
    }
}

