package com.bharath.stacked.modules.question_bank.dto;

import com.bharath.stacked.modules.question_bank.enums.BankQuestionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionBankItemRequest {

    @NotNull(message = "Question type is required.")
    private BankQuestionType questionType;

    @NotBlank(message = "Question ID is required.")
    private String questionId;

    private int order;

    private Double customPoints;
}

