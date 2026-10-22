package com.bharath.stacked.modules.assessment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssessmentValidationError {
    private String field;
    private String message;
    private String code;
    private String questionId;
}

