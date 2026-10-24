package com.bharath.stacked.modules.analytics.service;

import com.bharath.stacked.modules.analytics.model.AnalyticsDailyMetric;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class DailyMetricUpdater {

    private final MongoTemplate mongoTemplate;

    /**
     * Atomically increments a daily counter and optional numeric sum.
     */
    public void incrementDaily(LocalDate date, String category, String metricKey, long countDelta, double sumDelta) {
        String id = AnalyticsDailyMetric.buildId(date, metricKey);
        Query query = new Query(Criteria.where("_id").is(id));

        Update update = new Update()
                .setOnInsert("date", date)
                .setOnInsert("category", category)
                .setOnInsert("metricKey", metricKey)
                .inc("count", countDelta)
                .inc("numericSum", sumDelta)
                .set("updatedAt", Instant.now());

        mongoTemplate.upsert(query, update, AnalyticsDailyMetric.class);
    }

    /**
     * Atomically increments a sub-counter field within the daily metric.
     */
    public void incrementDailySubCounter(LocalDate date, String category, String metricKey, String subKey, long countDelta) {
        String id = AnalyticsDailyMetric.buildId(date, metricKey);
        Query query = new Query(Criteria.where("_id").is(id));

        Update update = new Update()
                .setOnInsert("date", date)
                .setOnInsert("category", category)
                .setOnInsert("metricKey", metricKey)
                .inc("count", countDelta)
                .inc("subCounters." + sanitizeSubKey(subKey), countDelta)
                .set("updatedAt", Instant.now());

        mongoTemplate.upsert(query, update, AnalyticsDailyMetric.class);
    }

    private String sanitizeSubKey(String key) {
        if (key == null) return "unknown";
        return key.replace(".", "_").replace("$", "_");
    }
}

