package com.bharath.stacked.modules.analytics.repository;

import com.bharath.stacked.modules.analytics.enums.AnalyticsEventType;
import com.bharath.stacked.modules.analytics.model.AnalyticsEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface AnalyticsEventRepository extends MongoRepository<AnalyticsEvent, String> {

    Optional<AnalyticsEvent> findByIdempotencyKey(String idempotencyKey);

    boolean existsByIdempotencyKey(String idempotencyKey);

    Page<AnalyticsEvent> findAllByOrderByTimestampDesc(Pageable pageable);

    Page<AnalyticsEvent> findAllByEventTypeOrderByTimestampDesc(AnalyticsEventType eventType, Pageable pageable);

    Page<AnalyticsEvent> findAllByAssessmentIdOrderByTimestampDesc(String assessmentId, Pageable pageable);

    long countByEventType(AnalyticsEventType eventType);

    long countByEventTypeAndTimestampBetween(AnalyticsEventType eventType, Instant start, Instant end);

    List<AnalyticsEvent> findAllByTimestampBetweenOrderByTimestampAsc(Instant start, Instant end);

    List<AnalyticsEvent> findAllByEventTypeAndTimestampBetweenOrderByTimestampAsc(AnalyticsEventType eventType, Instant start, Instant end);

    void deleteAllByTimestampBefore(Instant cutoff);
}

