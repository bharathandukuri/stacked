package com.bharath.stacked.modules.assessment.model;

import com.bharath.stacked.modules.assessment.enums.AttemptStatus;
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

@Document(collection = "assessment_attempts")
@CompoundIndexes({
        @CompoundIndex(name = "unique_attempt_per_number", def = "{'assessmentId': 1, 'candidateUserId': 1, 'attemptNumber': 1}", unique = true)
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssessmentAttempt {

    @Id
    private String id;

    @Indexed
    private String assessmentId;

    @Indexed
    private String assignmentId;

    @Indexed
    private String candidateUserId;

    @Indexed
    private String candidateEmail;

    private int attemptNumber;

    @Indexed
    @Builder.Default
    private AttemptStatus status = AttemptStatus.IN_PROGRESS;

    private Instant startedAt;

    @Indexed
    private Instant effectiveDeadline;

    private Instant submittedAt;

    private Double score;

    private Double maxPossibleScore;

    private Boolean passed;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}

