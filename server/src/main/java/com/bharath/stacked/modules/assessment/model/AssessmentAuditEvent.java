package com.bharath.stacked.modules.assessment.model;

import com.bharath.stacked.modules.assessment.enums.AssessmentAuditEventSource;
import com.bharath.stacked.modules.assessment.enums.AssessmentAuditEventType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Document(collection = "assessment_audit_events")
@CompoundIndexes({
        @CompoundIndex(name = "attempt_timestamp_idx", def = "{'attemptId': 1, 'timestamp': -1}"),
        @CompoundIndex(name = "assessment_timestamp_idx", def = "{'assessmentId': 1, 'timestamp': -1}"),
        @CompoundIndex(name = "attempt_event_type_idx", def = "{'attemptId': 1, 'eventType': 1}")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssessmentAuditEvent {

    @Id
    private String id;

    @Indexed
    private String assessmentId;

    @Indexed
    private String attemptId;

    @Indexed
    private String candidateUserId;

    private String candidateEmail;

    @Indexed
    private String questionId;

    private AssessmentAuditEventType eventType;

    private AssessmentAuditEventSource source;

    private String clientIp;

    private String userAgent;

    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();

    private Instant timestamp;

    @CreatedDate
    private Instant createdAt;
}

