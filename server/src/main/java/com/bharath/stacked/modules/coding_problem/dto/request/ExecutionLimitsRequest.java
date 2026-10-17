package com.bharath.stacked.modules.coding_problem.dto.request;

import com.bharath.stacked.modules.coding_problem.model.ExecutionLimits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ExecutionLimitsRequest {

    @Positive(message = "Time limit must be greater than zero")
    @Max(value = 60000, message = "Time limit cannot exceed 60000 ms")
    @Builder.Default
    private Long timeLimitMs = 2000L;

    @Positive(message = "Memory limit must be greater than zero")
    @Max(value = 4194304, message = "Memory limit cannot exceed 4194304 KB")
    @Builder.Default
    private Long memoryLimitKb = 262144L; // 256 MB in KB

    @Positive(message = "Output limit must be greater than zero")
    @Max(value = 104857600, message = "Output limit cannot exceed 100 MB")
    @Builder.Default
    private Long outputLimitBytes = 10485760L; // 10 MB in bytes

    @Positive(message = "Query timeout must be greater than zero")
    @Max(value = 60000, message = "Query timeout cannot exceed 60000 ms")
    @Builder.Default
    private Long queryTimeoutMs = 10000L; // 10s default for database queries

    public ExecutionLimits toExecutionLimits() {
        return ExecutionLimits.builder()
                .timeLimitMs(timeLimitMs != null ? timeLimitMs : 2000L)
                .memoryLimitKb(memoryLimitKb != null ? memoryLimitKb : 262144L)
                .outputLimitBytes(outputLimitBytes != null ? outputLimitBytes : 10485760L)
                .queryTimeoutMs(queryTimeoutMs != null ? queryTimeoutMs : 10000L)
                .build();
    }
}