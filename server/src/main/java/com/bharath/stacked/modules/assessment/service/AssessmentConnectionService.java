package com.bharath.stacked.modules.assessment.service;

import com.bharath.stacked.modules.assessment.model.AssessmentConnectionEvent;

public interface AssessmentConnectionService {

    record ConnectionRegistrationResult(boolean isNewConnection, boolean previousSessionInvalidated, String connectionId) {}

    ConnectionRegistrationResult registerConnection(
            String assessmentId,
            String attemptId,
            String candidateUserId,
            String connectionId,
            String clientIp,
            String userAgent
    );

    boolean recordHeartbeat(String assessmentId, String candidateUserId, String connectionId);

    void recordDisconnection(
            String assessmentId,
            String attemptId,
            String candidateUserId,
            String connectionId,
            String reason,
            boolean isAbnormal
    );

    boolean isAuthoritative(String assessmentId, String candidateUserId, String connectionId);
}

