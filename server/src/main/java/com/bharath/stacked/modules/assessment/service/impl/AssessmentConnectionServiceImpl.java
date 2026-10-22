package com.bharath.stacked.modules.assessment.service.impl;

import com.bharath.stacked.modules.assessment.enums.AssessmentAuditEventSource;
import com.bharath.stacked.modules.assessment.enums.AssessmentAuditEventType;
import com.bharath.stacked.modules.assessment.enums.ConnectionEventType;
import com.bharath.stacked.modules.assessment.model.AssessmentAuditEvent;
import com.bharath.stacked.modules.assessment.model.AssessmentConnectionEvent;
import com.bharath.stacked.modules.assessment.repository.AssessmentAuditEventRepository;
import com.bharath.stacked.modules.assessment.repository.AssessmentConnectionEventRepository;
import com.bharath.stacked.modules.assessment.service.AssessmentConnectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AssessmentConnectionServiceImpl implements AssessmentConnectionService {

    private static final String REDIS_KEY_PREFIX = "assessment:active_conn:";
    private static final Duration HEARTBEAT_TTL = Duration.ofSeconds(45);

    private final StringRedisTemplate stringRedisTemplate;
    private final AssessmentConnectionEventRepository connectionEventRepository;
    private final AssessmentAuditEventRepository auditEventRepository;

    private String buildRedisKey(String assessmentId, String candidateUserId) {
        return REDIS_KEY_PREFIX + assessmentId + ":" + candidateUserId;
    }

    @Override
    public ConnectionRegistrationResult registerConnection(
            String assessmentId,
            String attemptId,
            String candidateUserId,
            String connectionId,
            String clientIp,
            String userAgent) {

        String key = buildRedisKey(assessmentId, candidateUserId);
        String previousConnectionId = stringRedisTemplate.opsForValue().getAndSet(key, connectionId);
        stringRedisTemplate.expire(key, HEARTBEAT_TTL);

        boolean previousInvalidated = false;
        if (previousConnectionId != null && !previousConnectionId.equals(connectionId)) {
            previousInvalidated = true;
            log.info("Previous active connection [{}] for user [{}] was superseded by [{}]",
                    previousConnectionId, candidateUserId, connectionId);

            // Record STALE_REPLACED audit event
            connectionEventRepository.save(AssessmentConnectionEvent.builder()
                    .assessmentId(assessmentId)
                    .attemptId(attemptId)
                    .candidateUserId(candidateUserId)
                    .connectionId(previousConnectionId)
                    .eventType(ConnectionEventType.STALE_REPLACED)
                    .isIntegrityFlag(true)
                    .reason("Session superseded by new connection " + connectionId)
                    .clientIp(clientIp)
                    .userAgent(userAgent)
                    .timestamp(Instant.now())
                    .build());

            auditEventRepository.save(AssessmentAuditEvent.builder()
                    .assessmentId(assessmentId)
                    .attemptId(attemptId)
                    .candidateUserId(candidateUserId)
                    .eventType(AssessmentAuditEventType.STALE_SESSION_REPLACED)
                    .source(AssessmentAuditEventSource.SYSTEM_PRESENCE)
                    .clientIp(clientIp)
                    .userAgent(userAgent)
                    .metadata(Map.of("previousConnectionId", previousConnectionId != null ? previousConnectionId : "none", "newConnectionId", connectionId))
                    .timestamp(Instant.now())
                    .createdAt(Instant.now())
                    .build());
        }

        // Record CONNECTED or RECONNECTED event
        ConnectionEventType eventType = previousConnectionId != null ?
                ConnectionEventType.RECONNECTED : ConnectionEventType.CONNECTED;

        connectionEventRepository.save(AssessmentConnectionEvent.builder()
                .assessmentId(assessmentId)
                .attemptId(attemptId)
                .candidateUserId(candidateUserId)
                .connectionId(connectionId)
                .eventType(eventType)
                .isIntegrityFlag(false)
                .reason("Client established WebSocket connection")
                .clientIp(clientIp)
                .userAgent(userAgent)
                .timestamp(Instant.now())
                .build());

        auditEventRepository.save(AssessmentAuditEvent.builder()
                .assessmentId(assessmentId)
                .attemptId(attemptId)
                .candidateUserId(candidateUserId)
                .eventType(AssessmentAuditEventType.CONNECTION_RESTORED)
                .source(AssessmentAuditEventSource.SYSTEM_PRESENCE)
                .clientIp(clientIp)
                .userAgent(userAgent)
                .metadata(Map.of("connectionId", connectionId, "reconnected", previousConnectionId != null))
                .timestamp(Instant.now())
                .createdAt(Instant.now())
                .build());

        log.info("Registered active WebSocket connection [{}] for user [{}] in assessment [{}]",
                connectionId, candidateUserId, assessmentId);

        return new ConnectionRegistrationResult(true, previousInvalidated, connectionId);
    }

    @Override
    public boolean recordHeartbeat(String assessmentId, String candidateUserId, String connectionId) {
        String key = buildRedisKey(assessmentId, candidateUserId);
        String currentConnId = stringRedisTemplate.opsForValue().get(key);

        if (connectionId != null && connectionId.equals(currentConnId)) {
            stringRedisTemplate.expire(key, HEARTBEAT_TTL);
            return true;
        }

        log.warn("Heartbeat rejected for connection [{}]; current authoritative is [{}]",
                connectionId, currentConnId);
        return false;
    }

    @Override
    public void recordDisconnection(
            String assessmentId,
            String attemptId,
            String candidateUserId,
            String connectionId,
            String reason,
            boolean isAbnormal) {

        String key = buildRedisKey(assessmentId, candidateUserId);
        String currentConnId = stringRedisTemplate.opsForValue().get(key);

        // Atomic check: only clear Redis key if it belongs to this connection
        if (connectionId != null && connectionId.equals(currentConnId)) {
            stringRedisTemplate.delete(key);
            log.info("Authoritative connection [{}] closed and removed from Redis", connectionId);
        } else {
            log.debug("Non-authoritative connection [{}] closed; current authoritative is [{}]",
                    connectionId, currentConnId);
        }

        connectionEventRepository.save(AssessmentConnectionEvent.builder()
                .assessmentId(assessmentId)
                .attemptId(attemptId)
                .candidateUserId(candidateUserId)
                .connectionId(connectionId)
                .eventType(ConnectionEventType.DISCONNECTED)
                .isIntegrityFlag(isAbnormal)
                .reason(reason != null ? reason : "Connection closed")
                .timestamp(Instant.now())
                .build());

        auditEventRepository.save(AssessmentAuditEvent.builder()
                .assessmentId(assessmentId)
                .attemptId(attemptId)
                .candidateUserId(candidateUserId)
                .eventType(AssessmentAuditEventType.CONNECTION_INTERRUPTED)
                .source(AssessmentAuditEventSource.SYSTEM_PRESENCE)
                .metadata(Map.of("connectionId", connectionId != null ? connectionId : "none", "reason", reason != null ? reason : "Disconnected", "isAbnormal", isAbnormal))
                .timestamp(Instant.now())
                .createdAt(Instant.now())
                .build());
    }

    @Override
    public boolean isAuthoritative(String assessmentId, String candidateUserId, String connectionId) {
        String key = buildRedisKey(assessmentId, candidateUserId);
        String currentConnId = stringRedisTemplate.opsForValue().get(key);
        return connectionId != null && connectionId.equals(currentConnId);
    }
}

