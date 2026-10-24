package com.bharath.stacked.modules.analytics.repository;

import com.bharath.stacked.modules.analytics.model.AnalyticsDailyMetric;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AnalyticsDailyMetricRepository extends MongoRepository<AnalyticsDailyMetric, String> {

    Optional<AnalyticsDailyMetric> findByDateAndMetricKey(LocalDate date, String metricKey);

    List<AnalyticsDailyMetric> findAllByDateBetweenOrderByDateAsc(LocalDate startDate, LocalDate endDate);

    List<AnalyticsDailyMetric> findAllByCategoryAndDateBetweenOrderByDateAsc(String category, LocalDate startDate, LocalDate endDate);

    List<AnalyticsDailyMetric> findAllByMetricKeyAndDateBetweenOrderByDateAsc(String metricKey, LocalDate startDate, LocalDate endDate);
}

