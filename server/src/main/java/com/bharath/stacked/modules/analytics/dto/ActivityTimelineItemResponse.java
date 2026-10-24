package com.bharath.stacked.modules.analytics.dto;

import com.bharath.stacked.modules.analytics.enums.AnalyticsActorType;
import com.bharath.stacked.modules.analytics.enums.AnalyticsEntityType;
import com.bharath.stacked.modules.analytics.enums.AnalyticsEventType;
import com.bharath.stacked.modules.analytics.model.AnalyticsEvent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityTimelineItemResponse {

    private String id;
    private AnalyticsEventType eventType;
    private AnalyticsEntityType entityType;
    private String entityId;
    private String actorId;
    private AnalyticsActorType actorType;
    private String assessmentId;
    private Instant timestamp;
    private String summary;
    private Map<String, Object> attributes;

    public static ActivityTimelineItemResponse fromEvent(AnalyticsEvent event) {
        if (event == null) return null;
        return ActivityTimelineItemResponse.builder()
                .id(event.getId())
                .eventType(event.getEventType())
                .entityType(event.getEntityType())
                .entityId(event.getEntityId())
                .actorId(event.getActorId())
                .actorType(event.getActorType())
                .assessmentId(event.getAssessmentId())
                .timestamp(event.getTimestamp())
                .summary(formatSummary(event))
                .attributes(event.getAttributes())
                .build();
    }

    private static String formatSummary(AnalyticsEvent event) {
        String actor = event.getActorId() != null ? event.getActorId() : "System";
        String entity = event.getEntityType() != null ? event.getEntityType().name() : "Resource";
        return switch (event.getEventType()) {
            case CODING_PROBLEM_CREATED -> "Coding problem created by " + actor;
            case CODING_PROBLEM_PUBLISHED -> "Coding problem published by " + actor;
            case CODING_PROBLEM_DELETED -> "Coding problem deleted by " + actor;
            case MCQ_CREATED -> "MCQ question created by " + actor;
            case MCQ_PUBLISHED -> "MCQ question published by " + actor;
            case MCQ_DELETED -> "MCQ question deleted by " + actor;
            case QUESTION_BANK_CREATED -> "Question bank created by " + actor;
            case QUESTION_BANK_PUBLISHED -> "Question bank published by " + actor;
            case QUESTION_BANK_DELETED -> "Question bank deleted by " + actor;
            case ASSESSMENT_CREATED -> "Assessment created by " + actor;
            case ASSESSMENT_ACTIVATED -> Boolean.TRUE.equals(event.getAttributes().get("isManual"))
                    ? "Assessment manually activated by administrator"
                    : "Assessment activated automatically according to schedule";
            case ASSESSMENT_COMPLETED -> Boolean.TRUE.equals(event.getAttributes().get("isManual"))
                    ? "Assessment manually completed by administrator"
                    : "Assessment completed automatically according to schedule";
            case CANDIDATE_INVITED -> "Candidate invited to assessment";
            case CANDIDATE_ENROLLED -> "Candidate joined assessment via invitation code";
            case ATTEMPT_STARTED -> "Candidate started exam attempt";
            case ATTEMPT_SUBMITTED -> "Candidate submitted exam attempt";
            case ATTEMPT_EXPIRED -> "Candidate attempt finalized upon deadline expiration";
            case CODE_SUBMISSION_EVALUATED -> "Code submission evaluated: " + event.getAttributes().getOrDefault("verdict", "COMPLETED");
            default -> event.getEventType().name() + " on " + entity;
        };
    }
}

