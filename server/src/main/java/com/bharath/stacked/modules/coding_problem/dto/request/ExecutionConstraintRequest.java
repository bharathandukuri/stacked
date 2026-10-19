package com.bharath.stacked.modules.coding_problem.dto.request;

import com.bharath.stacked.modules.execution.dto.CodeExecutionConstraints;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

@Getter
@Setter
public class ExecutionConstraintRequest {

    @Positive(message = "Time limit must be greater than zero")
    @Max(value = 60000, message = "Time limit cannot exceed 60000 ms")
    private Long timeLimitMs;

    @Positive(message = "Memory limit must be greater than zero")
    @Max(value = 1048576, message = "Memory limit cannot exceed 1048576 KB")
    private Long memoryLimitKb;

    public ExecutionConstraintRequest() {
    }

    public ExecutionConstraintRequest(Long timeLimitMs, Long memoryLimitKb) {
        this.timeLimitMs = timeLimitMs;
        this.memoryLimitKb = memoryLimitKb;
    }

    public CodeExecutionConstraints toCodeExecutionConstraints() {
        return new CodeExecutionConstraints(
                timeLimitMs != null ? timeLimitMs : 0L,
                memoryLimitKb
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExecutionConstraintRequest that = (ExecutionConstraintRequest) o;
        return Objects.equals(timeLimitMs, that.timeLimitMs) &&
               Objects.equals(memoryLimitKb, that.memoryLimitKb);
    }

    @Override
    public int hashCode() {
        return Objects.hash(timeLimitMs, memoryLimitKb);
    }

    @Override
    public String toString() {
        return "ExecutionConstraintRequest{" +
               "timeLimitMs=" + timeLimitMs +
               ", memoryLimitKb=" + memoryLimitKb +
               '}';
    }
}
