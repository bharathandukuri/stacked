package com.bharath.stacked.modules.question_bank.dto;

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
public class HydratedQuestionItemResponse {
    private BankQuestionType questionType;
    private String questionId;
    private int order;
    private Double customPoints;
    private double effectivePoints;
    private String title;
    private String slug;
    private String snippet;
    private String difficulty;
    private List<String> topics;
}

