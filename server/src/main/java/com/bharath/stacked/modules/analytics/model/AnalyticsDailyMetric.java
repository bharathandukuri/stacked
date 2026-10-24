package com.bharath.stacked.modules.analytics.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "analytics_daily_metrics")
@CompoundIndexes({
        @CompoundIndex(name = "idx_date_category", def = "{'date': 1, 'category': 1}"),
        @CompoundIndex(name = "idx_date_metric", def = "{'date': 1, 'metricKey': 1}", unique = true)
})
public class AnalyticsDailyMetric {

    @Id
    private String id; // Format: {date}_{metricKey} e.g. "2026-10-24_ATTEMPT_SUBMITTED"

    private LocalDate date;

    private String category; // "CONTENT", "ASSESSMENT", "SUBMISSION"

    private String metricKey;

    @Builder.Default
    private long count = 0L;

    @Builder.Default
    private double numericSum = 0.0;

    @Builder.Default
    private Map<String, Long> subCounters = new HashMap<>();

    @Builder.Default
    private Instant updatedAt = Instant.now();

    public static String buildId(LocalDate date, String metricKey) {
        return date.toString() + "_" + metricKey;
    }
}

