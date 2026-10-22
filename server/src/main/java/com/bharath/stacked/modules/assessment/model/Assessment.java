package com.bharath.stacked.modules.assessment.model;

import com.bharath.stacked.modules.assessment.enums.AssessmentSelectionMode;
import com.bharath.stacked.modules.assessment.enums.AssessmentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "assessments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Assessment {

    @Id
    private String id;

    @Indexed(unique = true)
    private String slug;

    private String title;

    private String description;

    @Indexed
    @Builder.Default
    private AssessmentStatus status = AssessmentStatus.NOT_STARTED;

    @Builder.Default
    private AssessmentSelectionMode selectionMode = AssessmentSelectionMode.QUESTION_BANK;

    private String questionBankId;

    @Builder.Default
    private List<AssessmentItem> items = new ArrayList<>();

    @Indexed
    private Instant scheduledStart;

    @Indexed
    private Instant scheduledEnd;

    // Single overall assessment duration in minutes
    private int durationMinutes;

    // Optional limit: null or 0 means unlimited
    private Integer maxAttempts;

    private Double passingScore;

    private double totalScore;

    private int totalQuestions;

    private int mcqCount;

    private int codingCount;

    private int assignedCandidateCount;

    // Invitation Code Access
    @Indexed(sparse = true)
    private String invitationCode;

    @Builder.Default
    private boolean invitationCodeEnabled = false;

    private Instant invitationCodeExpiresAt;

    private Integer invitationCodeMaxUses;

    private int invitationCodeUsedCount;

    private String authorId;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    public AssessmentStatus getEffectiveStatus() {
        return getEffectiveStatus(Instant.now());
    }

    public AssessmentStatus getEffectiveStatus(Instant now) {
        return AssessmentStatus.deriveStatus(this.scheduledStart, this.scheduledEnd, now);
    }
}

