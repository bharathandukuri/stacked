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
public class MarkQuestionForReviewRequest {

    @NotBlank(message = "Question ID is required")
    private String questionId;

    private boolean markedForReview;
}

