package com.bharath.stacked.modules.coding_problem.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SubmitCodingProblemRequest {

    @NotBlank(message = "Language ID cannot be blank")
    private String languageId;

    @NotBlank(message = "Code cannot be blank")
    private String code;

    public SubmitCodingProblemRequest() {
    }

    public SubmitCodingProblemRequest(String languageId, String code) {
        this.languageId = languageId;
        this.code = code;
    }
}
