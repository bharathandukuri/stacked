package com.bharath.stacked.modules.assessment.dto;

import com.bharath.stacked.modules.question_bank.enums.BankQuestionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssessmentMetricsResponse {

    private String assessmentId;
    private String assessmentTitle;

    private int totalCandidatesAssigned;
    private int invitedCount;
    private int inProgressCount;
    private int submittedCount;
    private int expiredCount;

    private double completionRate;
    private double passRate;

    private double averageScore;
    private double medianScore;
    private double highestScore;
    private double lowestScore;
    private double maxPossibleScore;

    private double averageTimeSpentMinutes;

    private long totalIntegrityFlags;
    private long totalCodeSubmissions;
    private double codingSuccessRate;

    @Builder.Default
    private List<QuestionMetricDto> questionMetrics = new ArrayList<>();
}

