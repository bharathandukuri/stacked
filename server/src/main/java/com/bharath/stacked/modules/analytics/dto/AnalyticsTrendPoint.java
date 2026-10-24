package com.bharath.stacked.modules.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsTrendPoint {
    private LocalDate date;
    private String metricKey;
    private long count;
    private double value;
}

