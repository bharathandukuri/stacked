package com.bharath.stacked.modules.assessment.dto;

import com.bharath.stacked.modules.coding_problem.dto.response.CodingProblemExecutionResponse;
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
public class SavedQuestionResponseDto {

    private String questionId;
    private BankQuestionType questionType;
    @Builder.Default
    private List<String> selectedOptionIds = new ArrayList<>();
    private String languageId;
    private String code;
    private CodingProblemExecutionResponse lastExecutionResult;
    private Boolean allTestcasesPassed;
    private boolean markedForReview;
    private Instant savedAt;
}

