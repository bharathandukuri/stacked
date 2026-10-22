package com.bharath.stacked.modules.assessment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssessmentValidationResponse {
    private boolean valid;
    @Builder.Default
    private List<AssessmentValidationError> errors = new ArrayList<>();
    @Builder.Default
    private List<String> warnings = new ArrayList<>();
    private QuestionBankPreviewDto bankPreview;
    private Double calculatedTotalScore;
    private Integer calculatedTotalQuestions;
    private Integer calculatedDurationMinutes;
    private Integer candidateCount;
}

