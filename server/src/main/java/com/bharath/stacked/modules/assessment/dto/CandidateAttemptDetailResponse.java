package com.bharath.stacked.modules.assessment.dto;

import com.bharath.stacked.modules.assessment.enums.AttemptStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CandidateAttemptDetailResponse {

    private String attemptId;
    private String assessmentId;
    private String slug;
    private String title;
    private String description;
    private int attemptNumber;
    private AttemptStatus status;
    private Instant startedAt;
    private Instant effectiveDeadline;
    private long remainingSeconds;
    private int totalQuestions;
    private double totalScore;

    @Builder.Default
    private List<AssessmentItemResponse> questions = new ArrayList<>();

    @Builder.Default
    private Map<String, SavedQuestionResponseDto> responses = new HashMap<>();
}

