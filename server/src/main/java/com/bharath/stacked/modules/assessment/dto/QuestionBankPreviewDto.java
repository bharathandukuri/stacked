package com.bharath.stacked.modules.assessment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionBankPreviewDto {
    private String id;
    private String slug;
    private String title;
    private String description;
    private String category;
    private String status;
    private int totalQuestions;
    private int mcqCount;
    private int codingCount;
    private double totalPoints;
    @Builder.Default
    private Map<String, Integer> difficultyDistribution = new HashMap<>();
    @Builder.Default
    private List<String> supportedCodingLanguages = new ArrayList<>();
    @Builder.Default
    private List<QuestionBankPreviewItemDto> items = new ArrayList<>();
    private boolean eligibleForAssessment;
    private String ineligibilityReason;
}

