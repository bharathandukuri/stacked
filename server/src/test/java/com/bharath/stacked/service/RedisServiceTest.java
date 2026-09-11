package com.bharath.stacked.service;

import com.bharath.stacked.service.impl.RedisServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private RedisConnection redisConnection;

    private ObjectMapper objectMapper;
    private RedisService redisService;

    record UserDto(String id, String username, String email) {
    }

    @BeforeEach
    void setUp() {
        objectMapper = JsonMapper.builder().build();
        redisService = new RedisServiceImpl(redisTemplate, objectMapper);
    }

    @Nested
    @DisplayName("set() operations")
    class SetOperations {

        @Test
        @DisplayName("set without TTL stores value successfully")
        void set_WithoutTtl_Success() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);

            redisService.set("app:status", "active");

            verify(valueOperations).set("app:status", "active");
        }

        @Test
        @DisplayName("set without TTL rethrows exception when Redis fails")
        void set_WithoutTtl_RethrowsException() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            doThrow(new RedisSystemException("Redis down", new RuntimeException()))
                    .when(valueOperations).set("key", "value");

            assertThrows(RedisSystemException.class, () -> redisService.set("key", "value"));
        }

        @Test
        @DisplayName("set with Duration TTL stores value with expiration")
        void set_WithTtl_Success() {
            Duration ttl = Duration.ofMinutes(15);
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);

            redisService.set("session:token", "abc-123", ttl);

            verify(valueOperations).set("session:token", "abc-123", ttl);
        }

        @Test
        @DisplayName("set with TTL rethrows exception when Redis fails")
        void set_WithTtl_RethrowsException() {
            Duration ttl = Duration.ofSeconds(30);
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            doThrow(new RedisSystemException("Connection refused", new RuntimeException()))
                    .when(valueOperations).set("key", "val", ttl);

            assertThrows(RedisSystemException.class, () -> redisService.set("key", "val", ttl));
        }
    }

    @Nested
    @DisplayName("get() operations")
    class GetOperations {

        @Test
        @DisplayName("get raw object returns stored object when found")
        void get_Found() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("config:mode")).thenReturn("PRODUCTION");

            Object result = redisService.get("config:mode");

            assertEquals("PRODUCTION", result);
        }

        @Test
        @DisplayName("get raw object returns null when key does not exist")
        void get_NotFound() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("nonexistent")).thenReturn(null);

            Object result = redisService.get("nonexistent");

            assertNull(result);
        }

        @Test
        @DisplayName("get raw object catches exception and returns null")
        void get_ExceptionHandled() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("corrupted")).thenThrow(new RuntimeException("Socket closed"));

            Object result = redisService.get("corrupted");

            assertNull(result);
        }

        @Test
        @DisplayName("get typed returns null when key is absent")
        void getTyped_NullValue() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("missing")).thenReturn(null);

            String result = redisService.get("missing", String.class);

            assertNull(result);
        }

        @Test
        @DisplayName("get typed returns directly cast instance when matching target class")
        void getTyped_ExactInstance() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("counter")).thenReturn("100");

            String result = redisService.get("counter", String.class);

            assertEquals("100", result);
        }

        @Test
        @DisplayName("get typed converts Map to target DTO class via Jackson")
        void getTyped_JacksonConversion() {
            Map<String, Object> rawData = Map.of(
                    "id", "user-42",
                    "username", "bharath",
                    "email", "bharath@example.com");
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("user:42")).thenReturn(rawData);

            UserDto user = redisService.get("user:42", UserDto.class);

            assertNotNull(user);
            assertEquals("user-42", user.id());
            assertEquals("bharath", user.username());
            assertEquals("bharath@example.com", user.email());
        }

        @Test
        @DisplayName("get typed returns null when conversion fails or exception occurs")
        void getTyped_ConversionException_ReturnsNull() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("bad-type")).thenReturn("not-an-integer-or-map");

            UserDto result = redisService.get("bad-type", UserDto.class);

            assertNull(result);
        }
    }

    @Nested
    @DisplayName("hasKey() operations")
    class HasKeyOperations {

        @Test
        @DisplayName("hasKey returns true when key exists")
        void hasKey_True() {
            when(redisTemplate.hasKey("existing-key")).thenReturn(Boolean.TRUE);

            assertTrue(redisService.hasKey("existing-key"));
        }

        @Test
        @DisplayName("hasKey returns false when key is absent")
        void hasKey_False() {
            when(redisTemplate.hasKey("absent-key")).thenReturn(Boolean.FALSE);

            assertFalse(redisService.hasKey("absent-key"));
        }

        @Test
        @DisplayName("hasKey returns false when redisTemplate returns null")
        void hasKey_NullSafety() {
            when(redisTemplate.hasKey("null-result")).thenReturn(null);

            assertFalse(redisService.hasKey("null-result"));
        }

        @Test
        @DisplayName("hasKey catches exception and returns false")
        void hasKey_ExceptionHandled() {
            when(redisTemplate.hasKey("error-key")).thenThrow(new RuntimeException("Timeout"));

            assertFalse(redisService.hasKey("error-key"));
        }
    }

    @Nested
    @DisplayName("delete() operations")
    class DeleteOperations {

        @Test
        @DisplayName("delete returns true when key is successfully deleted")
        void delete_True() {
            when(redisTemplate.delete("key-to-del")).thenReturn(Boolean.TRUE);

            assertTrue(redisService.delete("key-to-del"));
        }

        @Test
        @DisplayName("delete returns false when key did not exist")
        void delete_False() {
            when(redisTemplate.delete("absent-key")).thenReturn(Boolean.FALSE);

            assertFalse(redisService.delete("absent-key"));
        }

        @Test
        @DisplayName("delete returns false when template returns null")
        void delete_NullSafety() {
            when(redisTemplate.delete("null-key")).thenReturn(null);

            assertFalse(redisService.delete("null-key"));
        }

        @Test
        @DisplayName("delete catches exception and returns false")
        void delete_ExceptionHandled() {
            when(redisTemplate.delete("broken-key")).thenThrow(new RuntimeException("Connection error"));

            assertFalse(redisService.delete("broken-key"));
        }
    }

    @Nested
    @DisplayName("expire() operations")
    class ExpireOperations {

        @Test
        @DisplayName("expire returns true when TTL is successfully applied")
        void expire_True() {
            Duration ttl = Duration.ofMinutes(5);
            when(redisTemplate.expire("session:1", ttl)).thenReturn(Boolean.TRUE);

            assertTrue(redisService.expire("session:1", ttl));
        }

        @Test
        @DisplayName("expire returns false when key does not exist")
        void expire_False() {
            Duration ttl = Duration.ofMinutes(5);
            when(redisTemplate.expire("missing:1", ttl)).thenReturn(Boolean.FALSE);

            assertFalse(redisService.expire("missing:1", ttl));
        }

        @Test
        @DisplayName("expire returns false when template returns null")
        void expire_NullSafety() {
            Duration ttl = Duration.ofSeconds(10);
            when(redisTemplate.expire("null:1", ttl)).thenReturn(null);

            assertFalse(redisService.expire("null:1", ttl));
        }

        @Test
        @DisplayName("expire catches exception and returns false")
        void expire_ExceptionHandled() {
            Duration ttl = Duration.ofMinutes(1);
            when(redisTemplate.expire("err:1", ttl)).thenThrow(new RuntimeException("IO Error"));

            assertFalse(redisService.expire("err:1", ttl));
        }
    }

    @Nested
    @DisplayName("getExpire() operations")
    class GetExpireOperations {

        @Test
        @DisplayName("getExpire returns positive TTL in seconds")
        void getExpire_Success() {
            when(redisTemplate.getExpire("ttl:key", TimeUnit.SECONDS)).thenReturn(300L);

            assertEquals(300L, redisService.getExpire("ttl:key"));
        }

        @Test
        @DisplayName("getExpire returns -1 when key has no TTL")
        void getExpire_NoTtl() {
            when(redisTemplate.getExpire("persist:key", TimeUnit.SECONDS)).thenReturn(-1L);

            assertEquals(-1L, redisService.getExpire("persist:key"));
        }

        @Test
        @DisplayName("getExpire catches exception and returns -1L")
        void getExpire_ExceptionHandled() {
            when(redisTemplate.getExpire("err:key", TimeUnit.SECONDS)).thenThrow(new RuntimeException("Network issue"));

            assertEquals(-1L, redisService.getExpire("err:key"));
        }
    }

    @Nested
    @DisplayName("ping() operations")
    @SuppressWarnings("unchecked")
    class PingOperations {

        private static RedisCallback<Boolean> anyCallback() {
            return any(RedisCallback.class);
        }

        @Test
        @DisplayName("ping returns true when connection responds with PONG")
        void ping_Pong_ReturnsTrue() {
            when(redisTemplate.execute(anyCallback())).thenAnswer(invocation -> {
                RedisCallback<Boolean> callback = invocation.getArgument(0);
                return callback.doInRedis(redisConnection);
            });
            when(redisConnection.ping()).thenReturn("PONG");

            assertTrue(redisService.ping());
        }

        @Test
        @DisplayName("ping returns true for case-insensitive pong")
        void ping_CaseInsensitivePong_ReturnsTrue() {
            when(redisTemplate.execute(anyCallback())).thenAnswer(invocation -> {
                RedisCallback<Boolean> callback = invocation.getArgument(0);
                return callback.doInRedis(redisConnection);
            });
            when(redisConnection.ping()).thenReturn("pong");

            assertTrue(redisService.ping());
        }

        @Test
        @DisplayName("ping returns false when connection responds with unexpected value")
        void ping_NonPong_ReturnsFalse() {
            when(redisTemplate.execute(anyCallback())).thenAnswer(invocation -> {
                RedisCallback<Boolean> callback = invocation.getArgument(0);
                return callback.doInRedis(redisConnection);
            });
            when(redisConnection.ping()).thenReturn("ERROR");

            assertFalse(redisService.ping());
        }

        @Test
        @DisplayName("ping returns false when connection ping returns null")
        void ping_NullResponse_ReturnsFalse() {
            when(redisTemplate.execute(anyCallback())).thenAnswer(invocation -> {
                RedisCallback<Boolean> callback = invocation.getArgument(0);
                return callback.doInRedis(redisConnection);
            });
            when(redisConnection.ping()).thenReturn(null);

            assertFalse(redisService.ping());
        }

        @Test
        @DisplayName("ping returns false when execute returns null")
        void ping_ExecuteReturnsNull_ReturnsFalse() {
            when(redisTemplate.execute(anyCallback())).thenReturn(null);

            assertFalse(redisService.ping());
        }

        @Test
        @DisplayName("ping catches exception and returns false")
        void ping_ExceptionHandled() {
            when(redisTemplate.execute(anyCallback())).thenThrow(new RuntimeException("Connection refused"));

            assertFalse(redisService.ping());
        }
    }
}
