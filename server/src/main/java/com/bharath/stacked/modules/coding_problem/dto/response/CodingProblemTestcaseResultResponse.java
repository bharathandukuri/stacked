package com.bharath.stacked.modules.coding_problem.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CodingProblemTestcaseResultResponse {
    private String testCaseId;
    private Integer caseIndex;
    private String status; // "PASSED", "WRONG_ANSWER", "COMPILATION_ERROR", "RUNTIME_ERROR", "TIME_LIMIT_EXCEEDED", "MEMORY_LIMIT_EXCEEDED"
    private String stdin;
    private String dbInit;
    private String actualOutput;
    private String expectedOutput;
    private String stderr;
    private Long exitCode;
    private Long executionTimeMs;
    private Long runtimeMs;
    private Double memoryMb;
    private String error;
    private Boolean isCustom;
    private Boolean isSample;
    private List<String> logs;
    private Boolean passed;

    public Boolean getPassed() {
        if (passed != null) {
            return passed;
        }
        return "PASSED".equalsIgnoreCase(status);
    }

    public CodingProblemTestcaseResultResponse(
            String testCaseId,
            Integer caseIndex,
            String status,
            String stdin,
            String dbInit,
            String actualOutput,
            String expectedOutput,
            String stderr,
            Long exitCode,
            Long executionTimeMs,
            Long runtimeMs,
            Double memoryMb,
            String error,
            Boolean isCustom,
            Boolean isSample,
            List<String> logs
    ) {
        this(testCaseId, caseIndex, status, stdin, dbInit, actualOutput, expectedOutput, stderr,
                exitCode, executionTimeMs, runtimeMs, memoryMb, error, isCustom, isSample, logs,
                "PASSED".equalsIgnoreCase(status));
    }
}
