package com.bharath.stacked.modules.question_bank.dto;

import com.bharath.stacked.modules.question_bank.enums.QuestionBankStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateQuestionBankRequest {

    @NotBlank(message = "Title is required.")
    private String title;

    private String description;

    private String category;

    @NotNull(message = "Status is required.")
    private QuestionBankStatus status;

    private List<String> tags;

    @Valid
    private List<QuestionBankItemRequest> items;
}

