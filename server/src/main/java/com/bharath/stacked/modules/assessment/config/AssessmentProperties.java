package com.bharath.stacked.modules.assessment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.assessment")
public record AssessmentProperties(
        @DefaultValue("45s") Duration heartbeatTtl,
        @DefaultValue("60000") long sweepFixedDelayMs,
        @DefaultValue("30") int gracePeriodSeconds) {
}
