package com.bharath.stacked.modules.assessment.websocket;

import com.bharath.stacked.modules.assessment.service.AssessmentConnectionService;
import com.bharath.stacked.modules.security.util.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class AssessmentWebSocketHandler extends TextWebSocketHandler {

    private final AssessmentConnectionService connectionService;
    private final JwtTokenProvider jwtTokenProvider;
    private final ObjectMapper objectMapper;

    // Active local session registry mapping connectionId -> WebSocketSession
    private final Map<String, WebSocketSession> activeSessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        URI uri = session.getUri();
        if (uri == null || uri.getQuery() == null) {
            session.close(new CloseStatus(4000, "Missing connection parameters"));
            return;
        }

        Map<String, String> queryParams = parseQueryParams(uri.getQuery());
        String token = queryParams.get("token");
        String assessmentId = queryParams.get("assessmentId");
        String attemptId = queryParams.get("attemptId");

        if (!StringUtils.hasText(token) || !StringUtils.hasText(assessmentId) || !StringUtils.hasText(attemptId)) {
            session.close(new CloseStatus(4001, "Required parameters: token, assessmentId, attemptId"));
            return;
        }

        if (!jwtTokenProvider.validateToken(token)) {
            session.close(new CloseStatus(4003, "Invalid authentication token"));
            return;
        }

        String candidateUserId = jwtTokenProvider.getUserIdFromToken(token);
        if (!StringUtils.hasText(candidateUserId)) {
            session.close(new CloseStatus(4003, "Unable to extract user identity"));
            return;
        }

        String connectionId = UUID.randomUUID().toString();
        String clientIp = session.getRemoteAddress() != null ? session.getRemoteAddress().getHostString() : "unknown";

        session.getAttributes().put("connectionId", connectionId);
        session.getAttributes().put("assessmentId", assessmentId);
        session.getAttributes().put("attemptId", attemptId);
        session.getAttributes().put("candidateUserId", candidateUserId);

        var regResult = connectionService.registerConnection(
                assessmentId, attemptId, candidateUserId, connectionId, clientIp, "WebSocket Client"
        );

        activeSessions.put(connectionId, session);

        Map<String, Object> welcome = Map.of(
                "type", "CONNECTION_ACK",
                "connectionId", connectionId,
                "assessmentId", assessmentId,
                "attemptId", attemptId,
                "previousInvalidated", regResult.previousSessionInvalidated(),
                "timestamp", Instant.now().toString()
        );

        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(welcome)));
        log.info("WebSocket established: connectionId [{}], user [{}]", connectionId, candidateUserId);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String connectionId = (String) session.getAttributes().get("connectionId");
        String assessmentId = (String) session.getAttributes().get("assessmentId");
        String candidateUserId = (String) session.getAttributes().get("candidateUserId");

        if (connectionId == null || assessmentId == null || candidateUserId == null) {
            return;
        }

        try {
            JsonNode root = objectMapper.readTree(message.getPayload());
            String type = root.has("type") ? root.get("type").asText() : "";

            if ("HEARTBEAT".equalsIgnoreCase(type)) {
                boolean authoritative = connectionService.recordHeartbeat(assessmentId, candidateUserId, connectionId);
                if (authoritative) {
                    Map<String, Object> ack = Map.of(
                            "type", "HEARTBEAT_ACK",
                            "timestamp", Instant.now().toString()
                    );
                    session.sendMessage(new TextMessage(objectMapper.writeValueAsString(ack)));
                } else {
                    // Session was superseded by a newer tab or connection
                    Map<String, Object> superseded = Map.of(
                            "type", "SESSION_SUPERSEDED",
                            "message", "A new connection was established elsewhere. This session is now inactive."
                    );
                    session.sendMessage(new TextMessage(objectMapper.writeValueAsString(superseded)));
                    session.close(new CloseStatus(4009, "Superseded by newer connection"));
                }
            }
        } catch (Exception ex) {
            log.warn("Error processing WebSocket message from connection [{}]: {}", connectionId, ex.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String connectionId = (String) session.getAttributes().get("connectionId");
        String assessmentId = (String) session.getAttributes().get("assessmentId");
        String attemptId = (String) session.getAttributes().get("attemptId");
        String candidateUserId = (String) session.getAttributes().get("candidateUserId");

        if (connectionId != null) {
            activeSessions.remove(connectionId);
            boolean isAbnormal = status.getCode() != CloseStatus.NORMAL.getCode() && status.getCode() != 1000;
            connectionService.recordDisconnection(
                    assessmentId, attemptId, candidateUserId, connectionId, status.getReason(), isAbnormal
            );
            log.info("WebSocket disconnected: connectionId [{}], code [{}], reason [{}]",
                    connectionId, status.getCode(), status.getReason());
        }
    }

    private Map<String, String> parseQueryParams(String query) {
        Map<String, String> params = new HashMap<>();
        if (!StringUtils.hasText(query)) return params;
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            if (idx > 0 && idx < pair.length() - 1) {
                params.put(pair.substring(0, idx), pair.substring(idx + 1));
            }
        }
        return params;
    }
}

