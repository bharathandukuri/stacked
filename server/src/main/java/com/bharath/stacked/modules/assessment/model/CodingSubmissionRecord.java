package com.bharath.stacked.modules.assessment.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CodingSubmissionRecord {
    private String submissionId;
    private Instant submittedAt;
    private String languageId;
    private String code;
    private String status; // ACCEPTED, WRONG_ANSWER, TIME_LIMIT_EXCEEDED, etc.
    private int passedTestcases;
    private int totalTestcases;
    private double awardedPoints;
    private long executionTimeMs;
}

