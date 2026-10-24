package com.bharath.stacked.modules.assessment.service.impl;

import com.bharath.stacked.modules.assessment.config.AssessmentProperties;
import com.bharath.stacked.modules.assessment.enums.ConnectionEventType;
import com.bharath.stacked.modules.assessment.model.AssessmentConnectionEvent;
import com.bharath.stacked.modules.assessment.repository.AssessmentAuditEventRepository;
import com.bharath.stacked.modules.assessment.repository.AssessmentConnectionEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AssessmentConnectionServiceImplTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private AssessmentConnectionEventRepository connectionEventRepository;
    @Mock
    private AssessmentAuditEventRepository auditEventRepository;

    private AssessmentProperties assessmentProperties = new AssessmentProperties(Duration.ofSeconds(45), 60000L, 30);

    private AssessmentConnectionServiceImpl connectionService;

    @BeforeEach
    void setUp() {
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        connectionService = new AssessmentConnectionServiceImpl(
                stringRedisTemplate, connectionEventRepository, auditEventRepository, assessmentProperties);
    }

    @Test
    @DisplayName("registerConnection should set Redis key and detect stale previous connection")
    void shouldRegisterConnectionAndSupersedeStale() {
        String key = "assessment:active_conn:ass-1:user-1";
        when(valueOperations.getAndSet(eq(key), eq("conn-new"))).thenReturn("conn-old");

        var result = connectionService.registerConnection(
                "ass-1", "att-1", "user-1", "conn-new", "127.0.0.1", "Mozilla/5.0"
        );

        assertThat(result.isNewConnection()).isTrue();
        assertThat(result.previousSessionInvalidated()).isTrue();
        assertThat(result.connectionId()).isEqualTo("conn-new");
        verify(stringRedisTemplate).expire(eq(key), any(Duration.class));
        verify(connectionEventRepository, times(2)).save(any(AssessmentConnectionEvent.class));
    }

    @Test
    @DisplayName("recordHeartbeat should refresh TTL only if connection is authoritative")
    void shouldRefreshTtlForAuthoritativeHeartbeat() {
        String key = "assessment:active_conn:ass-1:user-1";
        when(valueOperations.get(key)).thenReturn("conn-valid");

        boolean ok = connectionService.recordHeartbeat("ass-1", "user-1", "conn-valid");
        boolean rejected = connectionService.recordHeartbeat("ass-1", "user-1", "conn-stale");

        assertThat(ok).isTrue();
        assertThat(rejected).isFalse();
        verify(stringRedisTemplate, times(1)).expire(eq(key), any(Duration.class));
    }

    @Test
    @DisplayName("recordDisconnection should delete Redis key if connection was authoritative")
    void shouldRemoveAuthoritativeOnDisconnect() {
        String key = "assessment:active_conn:ass-1:user-1";
        when(valueOperations.get(key)).thenReturn("conn-active");

        connectionService.recordDisconnection("ass-1", "att-1", "user-1", "conn-active", "Normal close", false);

        verify(stringRedisTemplate).delete(key);
        verify(connectionEventRepository).save(any(AssessmentConnectionEvent.class));
    }
}

