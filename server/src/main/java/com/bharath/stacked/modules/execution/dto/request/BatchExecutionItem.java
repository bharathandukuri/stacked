package com.bharath.stacked.modules.execution.dto.request;

import com.bharath.stacked.modules.execution.dto.CodeExecutionConstraints;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class BatchExecutionItem {
    private String executablePath;
    private List<String> args;
    private String stdin;
    private CodeExecutionConstraints constraints;

    public BatchExecutionItem(String executablePath, String stdin, CodeExecutionConstraints constraints) {
        this(executablePath, null, stdin, constraints);
    }
}
