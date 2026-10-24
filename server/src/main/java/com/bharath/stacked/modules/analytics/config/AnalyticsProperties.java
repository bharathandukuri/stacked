package com.bharath.stacked.modules.analytics.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "stacked.analytics")
public record AnalyticsProperties(
        @DefaultValue("true") boolean enabled,
        @DefaultValue("90") int retentionDays,
        @DefaultValue("365") int maxQueryDays,
        @DefaultValue("4") int asyncWorkers,
        @DefaultValue("1000") int queueCapacity
) {}

