package com.bharath.stacked.modules.assessment.dto;

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
public class QuestionBankPreviewItemDto {
    private String questionId;
    private BankQuestionType questionType;
    private String title;
    private String difficulty;
    private double points;
    private List<String> topics;
    private List<String> supportedLanguages;
    private boolean eligible;
    private String ineligibilityReason;
}

