package com.bharath.stacked.modules.question_bank.model;

import com.bharath.stacked.modules.question_bank.enums.BankQuestionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionBankItem {
    private BankQuestionType questionType;
    private String questionId;
    private int order;
    private Double customPoints;
}

