package com.bharath.stacked.modules.mcq.dto.response;

public record McqStatsResponse(
        long totalQuestions,
        long publishedQuestions,
        long draftQuestions,
        long singleChoiceCount,
        long multipleChoiceCount,
        long booleanCount,
        long easyCount,
        long mediumCount,
        long hardCount,
        long totalTopics) {
}
