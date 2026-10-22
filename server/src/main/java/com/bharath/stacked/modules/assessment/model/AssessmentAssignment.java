package com.bharath.stacked.modules.assessment.model;

import com.bharath.stacked.modules.assessment.enums.CandidateParticipationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "assessment_assignments")
@CompoundIndexes({
        @CompoundIndex(name = "unique_assessment_candidate_email", def = "{'assessmentId': 1, 'candidateEmail': 1}", unique = true)
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssessmentAssignment {

    @Id
    private String id;

    @Indexed
    private String assessmentId;

    @Indexed
    private String candidateEmail;

    @Indexed
    private String candidateUserId; // Nullable if candidate hasn't registered/logged in yet

    @Indexed
    @Builder.Default
    private CandidateParticipationStatus status = CandidateParticipationStatus.INVITED;

    private int attemptsCount;

    private String lastAttemptId;

    private Instant invitedAt;

    private Instant startedAt;

    private Instant completedAt;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}

