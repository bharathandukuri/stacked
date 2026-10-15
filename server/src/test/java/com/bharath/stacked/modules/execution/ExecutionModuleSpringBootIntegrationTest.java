package com.bharath.stacked.modules.execution;

import com.bharath.stacked.AbstractIntegrationTest;
import com.bharath.stacked.modules.execution.dto.CodeExecutionConstraints;
import com.bharath.stacked.modules.execution.dto.ManagedContainer;
import com.bharath.stacked.modules.execution.dto.request.SimpleCodeExecutionRequest;
import com.bharath.stacked.modules.execution.dto.response.SimpleCodeExecutionResult;
import com.bharath.stacked.modules.execution.enums.CodeExecutionStatus;
import com.bharath.stacked.modules.execution.registry.ManagedContainerRegistry;
import com.bharath.stacked.modules.execution.scheduler.ContainerCleanupScheduler;
import com.bharath.stacked.modules.execution.service.CodeExecutionService;
import com.bharath.stacked.modules.execution.service.DockerExecutionService;
import com.bharath.stacked.modules.language.Language;
import com.bharath.stacked.modules.language.registry.LanguageRegistry;
import com.bharath.stacked.service.RedisService;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Execution Module Full Spring Boot Integration Test with Testcontainers")
class ExecutionModuleSpringBootIntegrationTest extends AbstractIntegrationTest {

    @DynamicPropertySource
    static void enableDockerExecution(DynamicPropertyRegistry registry) {
        registry.add("stacked.execution.docker.enabled", () -> "true");
    }

    @Autowired
    private CodeExecutionService codeExecutionService;

    @Autowired
    private DockerExecutionService dockerExecutionService;

    @Autowired
    private ManagedContainerRegistry containerRegistry;

    @Autowired
    private LanguageRegistry languageRegistry;

    @Autowired
    private ContainerCleanupScheduler cleanupScheduler;

    @Autowired
    private RedisService redisService;

    private boolean dockerReady;

    @BeforeEach
    void checkDocker() {
        try {
            dockerReady = dockerExecutionService != null &&
                    dockerExecutionService.isImageExists(languageRegistry.get("python-3.12").dockerImageDetails());
        } catch (Exception e) {
            dockerReady = false;
        }
    }

    @Test
    @DisplayName("Verify Spring DI wiring of all execution module beans")
    void contextLoadsExecutionBeans() {
        assertNotNull(codeExecutionService, "CodeExecutionService bean must be wired");
        assertNotNull(dockerExecutionService, "DockerExecutionService bean must be wired");
        assertNotNull(containerRegistry, "ManagedContainerRegistry bean must be wired");
        assertNotNull(languageRegistry, "LanguageRegistry bean must be wired");
        assertNotNull(cleanupScheduler, "ContainerCleanupScheduler bean must be wired");
        assertTrue(redisService.ping(), "RedisService must ping successfully on live container");
        assertEquals(9, languageRegistry.getAll().size(), "All 9 languages must be registered in LanguageRegistry");
    }

    @Test
    @DisplayName("End-to-End: execute Python code via Spring-managed beans and verify live Redis container tracking")
    void executeCodeThroughSpringContext() {
        Assumptions.assumeTrue(dockerReady, "Docker daemon or python image not available; skipping");

        Language python = languageRegistry.get("python-3.12");

        SimpleCodeExecutionRequest request = SimpleCodeExecutionRequest.builder()
                .language(python)
                .code("print('TESTCONTAINERS_SPRING_SUCCESS')")
                .constraints(new CodeExecutionConstraints(5000L, 262144L))
                .build();

        SimpleCodeExecutionResult result = codeExecutionService.run(request);

        assertNotNull(result);
        assertEquals(CodeExecutionStatus.SUCCESS, result.executionStatus());
        assertEquals(0L, result.exitCode());
        assertThat(result.stdout().trim()).isEqualTo("TESTCONTAINERS_SPRING_SUCCESS");

        // Verify that live Redis has zero lingering containers after execution completes
        List<ManagedContainer> lingering = containerRegistry.findCreatedBefore(Instant.now().plusSeconds(60));
        assertThat(lingering).isEmpty();
    }

    @Test
    @DisplayName("End-to-End: execute SQLite database query through Spring context and verify cleanup in live Redis")
    void executeDatabaseThroughSpringContext() {
        Language sqlite = languageRegistry.get("sqlite-3");
        Assumptions.assumeTrue(dockerReady && dockerExecutionService.isImageExists(sqlite.dockerImageDetails()),
                "SQLite image not available; skipping");

        String dbInit = """
                CREATE TABLE candidates (id INTEGER PRIMARY KEY, name TEXT, score INTEGER);
                INSERT INTO candidates (name, score) VALUES ('Alice', 98), ('Bob', 87);
                """;
        String query = "SELECT name FROM candidates WHERE score > 90;";

        SimpleCodeExecutionRequest request = SimpleCodeExecutionRequest.builder()
                .language(sqlite)
                .code(dbInit + "\n" + query)
                .constraints(new CodeExecutionConstraints(5000L, 262144L))
                .build();

        SimpleCodeExecutionResult result = codeExecutionService.run(request);

        assertNotNull(result);
        assertEquals(CodeExecutionStatus.SUCCESS, result.executionStatus());
        assertEquals(0L, result.exitCode());
        assertThat(result.stdout().trim()).contains("Alice");
        assertThat(result.stdout().trim()).doesNotContain("Bob");

        // Verify Redis is clean
        List<ManagedContainer> lingering = containerRegistry.findCreatedBefore(Instant.now().plusSeconds(60));
        assertThat(lingering).isEmpty();
    }

    @Test
    @DisplayName("End-to-End: sweep expired containers via ContainerCleanupScheduler through Spring context")
    void sweepExpiredContainersThroughSpringContext() {
        String testId = "spring-orphan-" + UUID.randomUUID();
        ManagedContainer expired = new ManagedContainer(
                testId,
                "orphan-name",
                "execution/python:3.12",
                Instant.now().minus(Duration.ofMinutes(45))
        );

        containerRegistry.register(expired);
        assertTrue(containerRegistry.isManaged(testId), "Container must be registered in live Redis");

        // Execute scheduled sweep
        cleanupScheduler.cleanupExpiredContainers();

        // The container was registered in Redis but nonexistent in Docker.
        // Docker deleteContainer handles NotFoundException gracefully and unregisters from Redis!
        assertFalse(containerRegistry.isManaged(testId), "Expired container must be unregistered from live Redis");
    }
}
