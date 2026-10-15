package com.bharath.stacked.modules.execution.util;

import com.bharath.stacked.config.RedisConfig;
import com.bharath.stacked.modules.execution.registry.ManagedContainerRegistry;
import com.bharath.stacked.modules.execution.registry.impl.RedisManagedContainerRegistry;
import com.bharath.stacked.service.RedisService;
import com.bharath.stacked.service.impl.RedisServiceImpl;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import tools.jackson.databind.ObjectMapper;

/**
 * Singleton Testcontainers Redis provider for execution module integration tests.
 * Provides a real, live Redis container instance and wiring for RedisManagedContainerRegistry.
 */
public final class TestcontainersRedisRegistryProvider {

    private static final GenericContainer<?> REDIS_CONTAINER;
    private static final ManagedContainerRegistry REGISTRY;
    private static final RedisService REDIS_SERVICE;
    private static final StringRedisTemplate STRING_REDIS_TEMPLATE;

    static {
        if (System.getProperty("testcontainers.ryuk.disabled") == null) {
            System.setProperty("testcontainers.ryuk.disabled", "true");
        }

        REDIS_CONTAINER = new GenericContainer<>(DockerImageName.parse("redis:8-alpine"))
                .withExposedPorts(6379)
                .waitingFor(Wait.forListeningPort());
        REDIS_CONTAINER.start();

        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration(
                REDIS_CONTAINER.getHost(),
                REDIS_CONTAINER.getFirstMappedPort()
        );
        LettuceConnectionFactory connectionFactory = new LettuceConnectionFactory(redisConfig);
        connectionFactory.afterPropertiesSet();

        RedisConfig config = new RedisConfig();
        ObjectMapper objectMapper = new ObjectMapper();
        RedisTemplate<String, Object> redisTemplate = config.redisTemplate(connectionFactory, objectMapper);
        STRING_REDIS_TEMPLATE = config.stringRedisTemplate(connectionFactory);
        REDIS_SERVICE = new RedisServiceImpl(redisTemplate, objectMapper);

        REGISTRY = new RedisManagedContainerRegistry(REDIS_SERVICE, STRING_REDIS_TEMPLATE);
    }

    private TestcontainersRedisRegistryProvider() {}

    public static ManagedContainerRegistry registry() {
        return REGISTRY;
    }

    public static RedisService redisService() {
        return REDIS_SERVICE;
    }

    public static StringRedisTemplate stringRedisTemplate() {
        return STRING_REDIS_TEMPLATE;
    }

    public static GenericContainer<?> container() {
        return REDIS_CONTAINER;
    }
}

