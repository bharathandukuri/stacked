package com.bharath.stacked.modules.assessment.dto;

import com.bharath.stacked.modules.assessment.model.OptionSnapshot;
import com.bharath.stacked.modules.coding_problem.dto.response.CodingProblemExecutionResponse;
import com.bharath.stacked.modules.question_bank.enums.BankQuestionType;
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
    private boolean isAnswered;
    private Boolean isCorrect;
    private boolean markedForReview;

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
}

