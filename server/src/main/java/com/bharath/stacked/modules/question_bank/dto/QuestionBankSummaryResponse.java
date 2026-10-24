package com.bharath.stacked.modules.question_bank.dto;

import com.bharath.stacked.modules.question_bank.enums.QuestionBankStatus;
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
public class QuestionBankSummaryResponse {
    private String id;
    private String title;
    private String description;
    private String category;
    private QuestionBankStatus status;
    private List<String> tags;
    private int totalQuestions;
    private int mcqCount;
    private int codingCount;
    private double totalPoints;
    private Instant createdAt;
    private Instant updatedAt;
}

