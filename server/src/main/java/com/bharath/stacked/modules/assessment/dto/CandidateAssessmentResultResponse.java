package com.bharath.stacked.modules.assessment.dto;

import com.bharath.stacked.modules.assessment.enums.AttemptStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CandidateAssessmentResultResponse {

    private String assessmentId;
    private String title;
    private String description;

    private String attemptId;
    private int attemptNumber;
    private AttemptStatus status;

    private Instant startedAt;
    private Instant submittedAt;
    private long timeSpentSeconds;

    private double score;
    private double maxPossibleScore;
    private double percentage;
    private Double passingScore;
    private Boolean passed;

    private double accuracyPercentage;

    private int totalQuestions;
    private int answeredCount;
    private int unansweredCount;
    private int markedForReviewCount;

    private int mcqCount;
    private int codingCount;

    private boolean resultsVisible;

    private List<QuestionResultSummaryDto> questionResults;
}

