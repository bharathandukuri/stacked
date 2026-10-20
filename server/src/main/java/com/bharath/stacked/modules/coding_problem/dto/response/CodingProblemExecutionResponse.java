package com.bharath.stacked.modules.coding_problem.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CodingProblemExecutionResponse {
    private String overallStatus; // "ACCEPTED", "WRONG_ANSWER", "COMPILATION_ERROR", "RUNTIME_ERROR", "TIME_LIMIT_EXCEEDED", "MEMORY_LIMIT_EXCEEDED"
    private Long totalExecutionTimeMs;
    private Integer totalTestcases;
    private Integer passedTestcases;
    private Integer failedTestcases;
    private List<CodingProblemTestcaseResultResponse> results;

    public CodingProblemExecutionResponse() {
    }

    public CodingProblemExecutionResponse(
            String overallStatus,
            Long totalExecutionTimeMs,
            Integer totalTestcases,
            Integer passedTestcases,
            Integer failedTestcases,
            List<CodingProblemTestcaseResultResponse> results
    ) {
        this.overallStatus = overallStatus;
        this.totalExecutionTimeMs = totalExecutionTimeMs;
        this.totalTestcases = totalTestcases;
        this.passedTestcases = passedTestcases;
        this.failedTestcases = failedTestcases;
        this.results = results;
    }
}
