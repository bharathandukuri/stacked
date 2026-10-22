package com.bharath.stacked.modules.assessment.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RunAssessmentCustomInputRequest {

    @NotBlank(message = "Question ID is required")
    private String questionId;

    @NotBlank(message = "Language ID is required")
    private String languageId;

    @NotBlank(message = "Code is required")
    private String code;

    private String stdin;
}

