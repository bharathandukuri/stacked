package com.bharath.stacked.modules.coding_problem.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PreviewRunResponse {
    private String overallStatus; // "SUCCESS", "FAILED", "COMPILATION_ERROR"
    private Long totalExecutionTimeMs;
    private Integer totalTestcases;
    private Integer passedTestcases;
    private Integer failedTestcases;
    private String logs;
    private List<PreviewTestcaseResultResponse> results;
}

