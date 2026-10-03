package com.bharath.stacked.modules.execution.dto.request;

import com.bharath.stacked.modules.execution.dto.CodeExecutionConstraints;
import com.bharath.stacked.modules.language.Language;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimpleCodeExecutionRequest {
    private Language language;
    private String code;
    private String fileName;
    private String stdin;
    private CodeExecutionConstraints constraints;
}
