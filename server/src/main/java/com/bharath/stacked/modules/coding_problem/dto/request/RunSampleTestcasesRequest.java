package com.bharath.stacked.modules.coding_problem.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class RunSampleTestcasesRequest {

    @NotBlank(message = "Language ID cannot be blank")
    private String languageId;

    @NotBlank(message = "Code cannot be blank")
    private String code;

    private List<CustomTestcaseRequest> customTestcases;

    public RunSampleTestcasesRequest() {
    }

    public RunSampleTestcasesRequest(String languageId, String code, List<CustomTestcaseRequest> customTestcases) {
        this.languageId = languageId;
        this.code = code;
        this.customTestcases = customTestcases;
    }
}
