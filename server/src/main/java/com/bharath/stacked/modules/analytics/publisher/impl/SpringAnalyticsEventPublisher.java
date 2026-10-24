package com.bharath.stacked.modules.analytics.publisher.impl;

import com.bharath.stacked.modules.analytics.config.AnalyticsProperties;
import com.bharath.stacked.modules.analytics.handler.AnalyticsEventHandler;
import com.bharath.stacked.modules.analytics.model.AnalyticsEvent;
import com.bharath.stacked.modules.analytics.publisher.AnalyticsEventPublisher;
import com.bharath.stacked.modules.analytics.repository.AnalyticsEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SpringAnalyticsEventPublisher implements AnalyticsEventPublisher {

    private final AnalyticsEventRepository eventRepository;
    private final List<AnalyticsEventHandler> eventHandlers;
    private final AnalyticsProperties analyticsProperties;

    @EventListener
    public void onAnalyticsEvent(AnalyticsEvent event) {
        publish(event);
    }

    @Override
    public void publish(AnalyticsEvent event) {
        if (!analyticsProperties.enabled()) {
            log.trace("Analytics subsystem is disabled. Dropping event: {}", event != null ? event.getEventType() : null);
            return;
        }

        if (event == null || event.getEventType() == null) {
            log.warn("Attempted to publish null or invalid analytics event.");
            return;
        }

        try {
            // Ensure authoritative server-side metadata
            if (event.getId() == null || event.getId().isBlank()) {
                event.setId(UUID.randomUUID().toString());
            }
            if (event.getTimestamp() == null) {
                event.setTimestamp(Instant.now());
            }

            // Check idempotency if idempotency key is supplied
            if (event.getIdempotencyKey() != null && !event.getIdempotencyKey().isBlank()) {
                if (eventRepository.existsByIdempotencyKey(event.getIdempotencyKey())) {
                    log.debug("Duplicate analytics event detected with idempotency key: [{}]. Ignoring.", event.getIdempotencyKey());
                    return;
                }
            }

            // Persist the event
            AnalyticsEvent saved;
            try {
                saved = eventRepository.save(event);
            } catch (DuplicateKeyException e) {
                log.debug("Duplicate analytics event key constraint violated: [{}]. Ignoring.", event.getIdempotencyKey());
                return;
            }

            // Dispatch to registered event handlers
            for (AnalyticsEventHandler handler : eventHandlers) {
                if (handler.supports(saved.getEventType())) {
                    try {
                        handler.handle(saved);
                    } catch (Exception ex) {
                        log.error("Error executing analytics handler [{}] for event [{}]: {}",
                                handler.getClass().getSimpleName(), saved.getEventType(), ex.getMessage(), ex);
                    }
                }
            }
        } catch (Exception ex) {
            // Absolute failure containment: business workflows MUST NEVER fail due to analytics
            log.error("Failed to publish or process analytics event [{}]: {}", event.getEventType(), ex.getMessage(), ex);
        }
    }
}
