package com.bharath.stacked.modules.question_bank.dto;

import com.bharath.stacked.modules.question_bank.enums.QuestionBankStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateQuestionBankRequest {

    @NotBlank(message = "Title is required.")
    private String title;

    private String description;

    private String category;

    @Builder.Default
    private QuestionBankStatus status = QuestionBankStatus.DRAFT;

    private List<String> tags;

    @Valid
    private List<QuestionBankItemRequest> items;
}

