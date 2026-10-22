package com.bharath.stacked.modules.assessment.model;

import com.bharath.stacked.modules.coding_problem.dto.response.CodingProblemExecutionResponse;
import com.bharath.stacked.modules.question_bank.enums.BankQuestionType;
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
import java.util.ArrayList;
import java.util.List;

@Document(collection = "assessment_responses")
@CompoundIndexes({
        @CompoundIndex(name = "unique_attempt_question_response", def = "{'attemptId': 1, 'questionId': 1}", unique = true)
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssessmentQuestionResponse {

    @Id
    private String id;

    @Indexed
    private String attemptId;

    @Indexed
    private String assessmentId;

    @Indexed
    private String candidateUserId;

    @Indexed
    private String questionId;

    private BankQuestionType questionType;

    // MCQ Response data
    @Builder.Default
    private List<String> selectedOptionIds = new ArrayList<>();
    private Boolean isCorrect;

    // Coding Response data
    private String languageId;
    private String code;
    private CodingProblemExecutionResponse lastExecutionResult;
    private Boolean allTestcasesPassed;

    private double awardedPoints;
    private double maxPoints;

    @Builder.Default
    private boolean markedForReview = false;

    private Instant savedAt;

    private Instant evaluatedAt;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}

