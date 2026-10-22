package com.bharath.stacked.modules.assessment.dto;

import com.bharath.stacked.modules.question_bank.enums.BankQuestionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionMetricDto {

    private String questionId;
    private int index;
    private String title;
    private BankQuestionType questionType;
    private double maxPoints;
    private int answeredCount;
    private int correctCount;
    private double averageScore;
    private double successRate;
}

