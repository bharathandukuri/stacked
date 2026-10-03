package com.bharath.stacked.modules.execution.mapper;

import com.bharath.stacked.modules.execution.enums.CodeExecutionStatus;
import com.bharath.stacked.modules.execution.enums.IsolateExecutionStatus;
import org.springframework.stereotype.Component;

@Component
public class CodeExecutionStatusMapper {

    public CodeExecutionStatus map(IsolateExecutionStatus status) {
        if (status == null) {
            return CodeExecutionStatus.SYSTEM_ERROR;
        }
        return switch (status) {
            case SUCCESS -> CodeExecutionStatus.SUCCESS;
            case TIME_LIMIT_EXCEEDED -> CodeExecutionStatus.TIME_LIMIT_EXCEEDED;
            case MEMORY_LIMIT_EXCEEDED -> CodeExecutionStatus.MEMORY_LIMIT_EXCEEDED;
            case RUNTIME_ERROR -> CodeExecutionStatus.RUNTIME_ERROR;
            case SYSTEM_ERROR -> CodeExecutionStatus.SYSTEM_ERROR;
        };
    }
}
