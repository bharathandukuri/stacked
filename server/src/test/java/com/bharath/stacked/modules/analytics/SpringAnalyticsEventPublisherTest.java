package com.bharath.stacked.modules.analytics;

import com.bharath.stacked.modules.analytics.config.AnalyticsProperties;
import com.bharath.stacked.modules.analytics.enums.AnalyticsActorType;
import com.bharath.stacked.modules.analytics.enums.AnalyticsEntityType;
import com.bharath.stacked.modules.analytics.enums.AnalyticsEventType;
import com.bharath.stacked.modules.analytics.handler.AnalyticsEventHandler;
import com.bharath.stacked.modules.analytics.model.AnalyticsEvent;
import com.bharath.stacked.modules.analytics.publisher.impl.SpringAnalyticsEventPublisher;
import com.bharath.stacked.modules.analytics.repository.AnalyticsEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SpringAnalyticsEventPublisherTest {

    @Mock
    private AnalyticsEventRepository analyticsEventRepository;

    @Mock
    private AnalyticsEventHandler handler1;

    @Mock
    private AnalyticsEventHandler handler2;

    private AnalyticsProperties analyticsProperties;
    private SpringAnalyticsEventPublisher publisher;

    @BeforeEach
    void setUp() {
        analyticsProperties = new AnalyticsProperties(true, 90, 365, 4, 1000);
        publisher = new SpringAnalyticsEventPublisher(
                analyticsEventRepository,
                List.of(handler1, handler2),
                analyticsProperties
        );
    }

    @Test
    @DisplayName("Should persist event and dispatch to supporting handlers")
    void shouldPersistAndDispatchEvent() {
        AnalyticsEvent event = AnalyticsEvent.builder()
                .eventType(AnalyticsEventType.CODING_PROBLEM_CREATED)
                .entityType(AnalyticsEntityType.CODING_PROBLEM)
                .entityId("prob-1")
                .actorType(AnalyticsActorType.ADMIN)
                .actorId("admin-1")
                .timestamp(Instant.now())
                .attributes(Map.of("title", "Two Sum"))
                .build();

        when(analyticsEventRepository.save(any(AnalyticsEvent.class))).thenReturn(event);
        when(handler1.supports(AnalyticsEventType.CODING_PROBLEM_CREATED)).thenReturn(true);
        when(handler2.supports(AnalyticsEventType.CODING_PROBLEM_CREATED)).thenReturn(false);

        publisher.publish(event);

        verify(analyticsEventRepository, times(1)).save(event);
        verify(handler1, times(1)).handle(event);
        verify(handler2, never()).handle(any());
    }

    @Test
    @DisplayName("Should deduplicate events when idempotency key already exists")
    void shouldDeduplicateWhenIdempotencyKeyExists() {
        AnalyticsEvent event = AnalyticsEvent.builder()
                .eventType(AnalyticsEventType.ATTEMPT_STARTED)
                .entityType(AnalyticsEntityType.ATTEMPT)
                .entityId("attempt-1")
                .idempotencyKey("unique-key-123")
                .build();

        when(analyticsEventRepository.existsByIdempotencyKey("unique-key-123")).thenReturn(true);

        publisher.publish(event);

        verify(analyticsEventRepository, never()).save(any());
        verify(handler1, never()).handle(any());
        verify(handler2, never()).handle(any());
    }

    @Test
    @DisplayName("Should isolate handler exceptions and never fail caller")
    void shouldIsolateHandlerExceptions() {
        AnalyticsEvent event = AnalyticsEvent.builder()
                .eventType(AnalyticsEventType.ASSESSMENT_ACTIVATED)
                .entityType(AnalyticsEntityType.ASSESSMENT)
                .entityId("asmt-1")
                .build();

        when(analyticsEventRepository.save(any(AnalyticsEvent.class))).thenReturn(event);
        when(handler1.supports(AnalyticsEventType.ASSESSMENT_ACTIVATED)).thenReturn(true);
        doThrow(new RuntimeException("Simulated database failure inside handler"))
                .when(handler1).handle(any());

        when(handler2.supports(AnalyticsEventType.ASSESSMENT_ACTIVATED)).thenReturn(true);

        assertThatCode(() -> publisher.publish(event))
                .doesNotThrowAnyException();

        // Handler 2 should still be invoked even if Handler 1 failed
        verify(handler1, times(1)).handle(event);
        verify(handler2, times(1)).handle(event);
    }

    @Test
    @DisplayName("Should do nothing when analytics subsystem is disabled")
    void shouldDoNothingWhenDisabled() {
        AnalyticsProperties disabledProps = new AnalyticsProperties(false, 90, 365, 4, 1000);
        SpringAnalyticsEventPublisher disabledPublisher = new SpringAnalyticsEventPublisher(
                analyticsEventRepository,
                List.of(handler1),
                disabledProps
        );

        AnalyticsEvent event = AnalyticsEvent.builder()
                .eventType(AnalyticsEventType.MCQ_CREATED)
                .build();

        disabledPublisher.publish(event);

        verify(analyticsEventRepository, never()).save(any());
        verify(handler1, never()).handle(any());
    }
}

