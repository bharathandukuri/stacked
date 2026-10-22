package com.bharath.stacked.modules.assessment.dto;

import jakarta.validation.constraints.NotBlank;
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
public class SaveMcqResponseRequest {

    @NotBlank(message = "Question ID is required")
    private String questionId;

    @Builder.Default
    private List<String> selectedOptionIds = new ArrayList<>();
}

