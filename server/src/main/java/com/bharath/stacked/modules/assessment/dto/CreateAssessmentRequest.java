package com.bharath.stacked.modules.assessment.dto;

import com.bharath.stacked.modules.assessment.enums.AssessmentSelectionMode;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class CreateAssessmentRequest {

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    @Builder.Default
    private AssessmentSelectionMode selectionMode = AssessmentSelectionMode.QUESTION_BANK;

    private String questionBankId;

    @Builder.Default
    private List<AssessmentCustomItemRequest> customQuestionItems = new ArrayList<>();

    // Raw candidate emails string (comma or whitespace separated)
    private String candidateEmailsRaw;

    @Builder.Default
    private List<String> candidateEmails = new ArrayList<>();

    private Instant scheduledStart;

    private Instant scheduledEnd;

    @NotNull(message = "Duration is required")
    @Min(value = 1, message = "Duration must be at least 1 minute")
    private Integer durationMinutes;

    private Integer maxAttempts;

    private Double passingScore;

    @Builder.Default
    private Boolean enableInvitationCode = false;

    private Instant invitationCodeExpiresAt;

    private Integer invitationCodeMaxUses;

    public boolean isEnableInvitationCode() {
        return Boolean.TRUE.equals(enableInvitationCode);
    }
}

