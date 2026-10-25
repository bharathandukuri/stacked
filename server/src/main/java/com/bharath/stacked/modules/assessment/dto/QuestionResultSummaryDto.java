package com.bharath.stacked.modules.assessment.dto;

import com.bharath.stacked.modules.assessment.model.OptionSnapshot;
import com.bharath.stacked.modules.coding_problem.dto.response.CodingProblemExecutionResponse;
import com.bharath.stacked.modules.question_bank.enums.BankQuestionType;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionResultSummaryDto {

    private String questionId;
    private int questionIndex;
    private String title;
    private BankQuestionType questionType;
    private double pointsAwarded;
    private double maxPoints;

    @JsonProperty("isAnswered")
    private boolean isAnswered;

    @JsonProperty("isCorrect")
    private Boolean isCorrect;

    private boolean markedForReview;

    @JsonProperty("isAnswered")
    public boolean isAnswered() {
        return isAnswered;
    }

    @JsonProperty("answered")
    public boolean getAnswered() {
        return isAnswered;
    }

    // Content for review
    private String mcqStatement;
    private String codingStatement;

    // MCQ specific
    private List<String> selectedOptionIds;
    private List<OptionSnapshot> options;

    // Coding specific
    private String languageId;
    private String code;
    private CodingProblemExecutionResponse lastExecutionResult;
    private Boolean allTestcasesPassed;

    private Double bestAwardedPoints;
    private Integer bestPassedTestcases;
    private Integer totalTestcases;

    @Builder.Default
    private List<com.bharath.stacked.modules.assessment.model.CodingSubmissionRecord> submissions = new java.util.ArrayList<>();
}

