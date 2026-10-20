package com.bharath.stacked.modules.coding_problem.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
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

    public CodingProblemTestcaseResultResponse() {
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
        this.testCaseId = testCaseId;
        this.caseIndex = caseIndex;
        this.status = status;
        this.stdin = stdin;
        this.dbInit = dbInit;
        this.actualOutput = actualOutput;
        this.expectedOutput = expectedOutput;
        this.stderr = stderr;
        this.exitCode = exitCode;
        this.executionTimeMs = executionTimeMs;
        this.runtimeMs = runtimeMs;
        this.memoryMb = memoryMb;
        this.error = error;
        this.isCustom = isCustom;
        this.isSample = isSample;
        this.logs = logs;
    }
}
