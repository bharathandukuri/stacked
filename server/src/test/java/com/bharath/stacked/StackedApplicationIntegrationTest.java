package com.bharath.stacked;

import com.bharath.stacked.service.RedisService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("StackedApplication Context Integration Test")
class StackedApplicationIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private RedisService redisService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private RedisConnectionFactory redisConnectionFactory;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Context loads successfully with healthy live Testcontainers connections")
    void contextLoadsWithLiveContainers() {
        assertNotNull(applicationContext, "ApplicationContext must be present");
        assertNotNull(mongoTemplate, "MongoTemplate bean must be initialized");
        assertNotNull(redisService, "RedisService bean must be initialized");
        assertNotNull(redisTemplate, "RedisTemplate bean must be initialized");
        assertNotNull(redisConnectionFactory, "RedisConnectionFactory bean must be initialized");
        assertNotNull(objectMapper, "ObjectMapper (Jackson 3) bean must be initialized");

        // Verify live MongoDB connectivity
        assertDoesNotThrow(() -> mongoTemplate.getCollectionNames(), "MongoDB must be queryable without error");

        // Verify live Redis connectivity
        assertTrue(redisService.ping(), "Redis ping must succeed on live container");
    }
}
