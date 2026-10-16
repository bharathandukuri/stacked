package com.bharath.stacked.modules.execution.dto.request;

import com.bharath.stacked.modules.execution.dto.CodeExecutionConstraints;
import com.bharath.stacked.modules.language.Language;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class BatchCodeExecutionRequest {
    private Language language;

    //path, content
    private Map<String, String> files;

    // Default constraints for this batch
    private CodeExecutionConstraints constraints;

    // Individual executions to run
    private List<BatchExecutionItem> executions;
}
