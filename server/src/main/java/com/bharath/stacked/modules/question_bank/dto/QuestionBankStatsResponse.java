package com.bharath.stacked.modules.question_bank.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionBankStatsResponse {
    private long totalBanks;
    private long publishedBanks;
    private long draftBanks;
    private long totalCuratedQuestions;
    private long totalCategories;
}

