package com.bharath.stacked.service;

import com.bharath.stacked.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RedisService Live Integration Tests")
class RedisServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private RedisService redisService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    record UserSession(String userId, String username, String role) {
    }

    @BeforeEach
    void setUp() {
        var factory = stringRedisTemplate.getConnectionFactory();
        if (factory != null) {
            try (var connection = factory.getConnection()) {
                connection.serverCommands().flushDb();
            }
        }
    }

    @Test
    @DisplayName("ping() communicates with live Redis container successfully")
    void testLivePing() {
        boolean alive = redisService.ping();
        assertTrue(alive, "Redis ping should return true on running container");
    }

    @Test
    @DisplayName("set() and get() basic key-value in live Redis")
    void testSetAndGetBasic() {
        String key = "test:simple";
        String value = "hello-stacked";

        redisService.set(key, value);

        Object retrieved = redisService.get(key);
        assertEquals(value, retrieved);

        String typed = redisService.get(key, String.class);
        assertEquals(value, typed);
    }

    @Test
    @DisplayName("set() with Duration TTL enforces real expiration in Redis")
    void testSetWithTtl() {
        String key = "test:ttl-key";
        Duration ttl = Duration.ofSeconds(60);

        redisService.set(key, "temporary-data", ttl);

        Long remainingTtl = redisService.getExpire(key);
        assertNotNull(remainingTtl);
        assertTrue(remainingTtl > 0 && remainingTtl <= 60,
                "Remaining TTL should be between 1 and 60 seconds, but was: " + remainingTtl);
    }

    @Test
    @DisplayName("hasKey() and delete() accurately reflect live Redis state")
    void testHasKeyAndDelete() {
        String key = "test:lifecycle";
        assertFalse(redisService.hasKey(key));

        redisService.set(key, "data");
        assertTrue(redisService.hasKey(key));

        boolean deleted = redisService.delete(key);
        assertTrue(deleted);
        assertFalse(redisService.hasKey(key));
    }

    @Test
    @DisplayName("expire() updates expiration on an existing key")
    void testExpire() {
        String key = "test:update-ttl";
        redisService.set(key, "persistent-initially");

        assertEquals(-1L, redisService.getExpire(key), "Key should have no TTL initially");

        boolean expireSet = redisService.expire(key, Duration.ofMinutes(10));
        assertTrue(expireSet);

        Long ttl = redisService.getExpire(key);
        assertTrue(ttl > 0 && ttl <= 600);
    }

    @Test
    @DisplayName("Typed get() serializes and deserializes records via Jackson 3")
    void testTypedObjectSerialization() {
        String key = "session:user:101";
        UserSession originalSession = new UserSession("101", "bharath", "ADMIN");

        redisService.set(key, originalSession);

        UserSession retrievedSession = redisService.get(key, UserSession.class);
        assertNotNull(retrievedSession);
        assertEquals("101", retrievedSession.userId());
        assertEquals("bharath", retrievedSession.username());
        assertEquals("ADMIN", retrievedSession.role());
    }
}
