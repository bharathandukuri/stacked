package com.bharath.stacked.config.cache;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

@ConfigurationProperties(prefix = "stacked.cache")
public record CacheProperties(
        @DefaultValue("true") boolean enabled,
        @DefaultValue("PT5M") Duration defaultTtl,
        @DefaultValue("PT2M") Duration analyticsTtl,
        @DefaultValue("PT10M") Duration catalogPageTtl,
        @DefaultValue("PT30M") Duration detailsTtl,
        @DefaultValue("PT1H") Duration metadataTtl,
        @DefaultValue("PT5M") Duration metricsTtl
) {}

