package com.bharath.stacked.modules.question_bank.dto;

import com.bharath.stacked.modules.question_bank.enums.QuestionBankStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateQuestionBankStatusRequest {

    @NotNull(message = "Status cannot be null")
    private QuestionBankStatus status;
}

