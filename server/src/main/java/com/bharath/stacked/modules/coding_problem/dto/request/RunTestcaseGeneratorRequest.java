package com.bharath.stacked.modules.coding_problem.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
public class RunTestcaseGeneratorRequest {

    @NotBlank(message = "Generator language is required")
    private String language;

    @NotBlank(message = "Generator script cannot be empty")
    @Size(max = 65536, message = "Generator script must not exceed 64KB")
    private String script;

    @Size(max = 65536, message = "Stdin must not exceed 64KB")
    private String stdin;

    @Valid
    private ExecutionConstraintRequest constraints;

    public RunTestcaseGeneratorRequest() {
    }

    public RunTestcaseGeneratorRequest(String language, String script, String stdin, ExecutionConstraintRequest constraints) {
        this.language = language;
        this.script = script;
        this.stdin = stdin;
        this.constraints = constraints;
    }
}
