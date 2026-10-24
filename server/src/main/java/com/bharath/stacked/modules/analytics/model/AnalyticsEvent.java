package com.bharath.stacked.modules.analytics.model;

import com.bharath.stacked.modules.analytics.enums.AnalyticsActorType;
import com.bharath.stacked.modules.analytics.enums.AnalyticsEntityType;
import com.bharath.stacked.modules.analytics.enums.AnalyticsEventType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "analytics_events")
@CompoundIndexes({
        @CompoundIndex(name = "idx_event_type_timestamp", def = "{'eventType': 1, 'timestamp': -1}"),
        @CompoundIndex(name = "idx_entity_type_id", def = "{'entityType': 1, 'entityId': 1}"),
        @CompoundIndex(name = "idx_assessment_timestamp", def = "{'assessmentId': 1, 'timestamp': -1}"),
        @CompoundIndex(name = "idx_idempotency_key", def = "{'idempotencyKey': 1}", unique = true, sparse = true)
})
public class AnalyticsEvent {

    @Id
    private String id;

    @Indexed
    private AnalyticsEventType eventType;

    private AnalyticsEntityType entityType;

    @Indexed
    private String entityId;

    private String actorId;

    private AnalyticsActorType actorType;

    @Indexed
    private String assessmentId;

    private String attemptId;

    private String candidateEmail;

    @Indexed
    @Builder.Default
    private Instant timestamp = Instant.now();

    private String idempotencyKey;

    @Builder.Default
    private Map<String, Object> attributes = new HashMap<>();

    public AnalyticsEvent addAttribute(String key, Object value) {
        if (this.attributes == null) {
            this.attributes = new HashMap<>();
        }
        if (key != null && value != null) {
            this.attributes.put(key, value);
        }
        return this;
    }
}
