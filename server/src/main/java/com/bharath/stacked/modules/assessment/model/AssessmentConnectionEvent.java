package com.bharath.stacked.modules.assessment.model;

import com.bharath.stacked.modules.assessment.enums.ConnectionEventType;
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

@Document(collection = "assessment_connection_events")
@CompoundIndexes({
        @CompoundIndex(name = "attempt_timestamp_idx", def = "{'attemptId': 1, 'timestamp': -1}")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssessmentConnectionEvent {

    @Id
    private String id;

    @Indexed
    private String assessmentId;

    @Indexed
    private String attemptId;

    @Indexed
    private String candidateUserId;

    private String connectionId;

    private ConnectionEventType eventType;

    private boolean isIntegrityFlag;

    private String reason;

    private String clientIp;

    private String userAgent;

    private Instant timestamp;

    @CreatedDate
    private Instant createdAt;
}

