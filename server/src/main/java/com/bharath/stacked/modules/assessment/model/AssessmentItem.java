package com.bharath.stacked.modules.assessment.model;

import com.bharath.stacked.modules.question_bank.enums.BankQuestionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssessmentItem {
    private String questionId;
    private BankQuestionType questionType;
    private int order;
    private double points;
    private QuestionSnapshot snapshot;
}

