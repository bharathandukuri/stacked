package com.bharath.stacked.modules.assessment.dto;

import com.bharath.stacked.modules.assessment.enums.AssessmentSelectionMode;
import com.bharath.stacked.modules.assessment.enums.AssessmentStatus;
import com.bharath.stacked.modules.assessment.model.Assessment;
import com.bharath.stacked.modules.assessment.model.AssessmentItem;
import com.bharath.stacked.modules.question_bank.enums.BankQuestionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssessmentDetailResponse {

    private String id;
    private String slug;
    private String title;
    private String description;
    private AssessmentStatus status;
    private AssessmentSelectionMode selectionMode;
    private String questionBankId;
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
    @Builder.Default
    private List<AssessmentItemResponse> items = new ArrayList<>();
    private Instant createdAt;
    private Instant updatedAt;

    public static AssessmentDetailResponse fromModel(Assessment assessment) {
        if (assessment == null) return null;
        List<AssessmentItemResponse> itemResponses = new ArrayList<>();
        if (assessment.getItems() != null) {
            for (AssessmentItem item : assessment.getItems()) {
                itemResponses.add(AssessmentItemResponse.fromModel(item, true)); // Admin view includes details
            }
        }

        return AssessmentDetailResponse.builder()
                .id(assessment.getId())
                .slug(assessment.getSlug())
                .title(assessment.getTitle())
                .description(assessment.getDescription())
                .status(assessment.getEffectiveStatus())
                .selectionMode(assessment.getSelectionMode())
                .questionBankId(assessment.getQuestionBankId())
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
                .items(itemResponses)
                .createdAt(assessment.getCreatedAt())
                .updatedAt(assessment.getUpdatedAt())
                .build();
    }
}

