package com.bharath.stacked.modules.execution.dto.request;

import com.bharath.stacked.modules.execution.dto.CodeExecutionConstraints;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class BatchExecutionItem {
    private String executablePath;
    private String stdin;
    private CodeExecutionConstraints constraints;
}
