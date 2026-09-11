package com.bharath.stacked;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mongodb.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@SuppressWarnings("resource")
public abstract class AbstractIntegrationTest {

    private static final DockerImageName MONGO_IMAGE = DockerImageName.parse("mongo:8.0")
            .asCompatibleSubstituteFor("mongo");
    private static final DockerImageName REDIS_IMAGE = DockerImageName.parse("redis:8-alpine");

    protected static final MongoDBContainer MONGO_CONTAINER;
    protected static final GenericContainer<?> REDIS_CONTAINER;

    static {
        MONGO_CONTAINER = new MongoDBContainer(MONGO_IMAGE);
        MONGO_CONTAINER.start();

        REDIS_CONTAINER = new GenericContainer<>(REDIS_IMAGE)
                .withExposedPorts(6379)
                .waitingFor(Wait.forListeningPort());
        REDIS_CONTAINER.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        String mongoUri = MONGO_CONTAINER.getConnectionString() + "/stacked_test_db";
        registry.add("spring.mongodb.uri", () -> mongoUri);
        registry.add("spring.data.mongodb.uri", () -> mongoUri);
        registry.add("MONGODB_URI", () -> mongoUri);

        String redisHost = REDIS_CONTAINER.getHost();
        Integer redisPort = REDIS_CONTAINER.getFirstMappedPort();

        registry.add("spring.data.redis.host", () -> redisHost);
        registry.add("spring.data.redis.port", () -> redisPort);
        registry.add("spring.data.redis.password", () -> "");
        registry.add("REDIS_HOST", () -> redisHost);
        registry.add("REDIS_PORT", () -> redisPort);
        registry.add("REDIS_PASSWORD", () -> "");
    }
}
