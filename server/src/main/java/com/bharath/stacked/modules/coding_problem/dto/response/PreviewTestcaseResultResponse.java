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
public class PreviewTestcaseResultResponse {
    private String testCaseId;
    private Integer caseIndex;
    private String status; // "SUCCESS", "PASSED", "FAILED", "COMPILATION_ERROR", "RUNTIME_ERROR", "TIME_LIMIT_EXCEEDED", "MEMORY_LIMIT_EXCEEDED"
    private String stdout;
    private String stderr;
    private Long exitCode;
    private Long runtimeMs;
    private Double memoryMb;
    private String expectedOutput;
    private List<String> logs;
    private String error;
}

