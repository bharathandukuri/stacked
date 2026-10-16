package com.bharath.stacked.modules.execution.integration;

import com.bharath.stacked.modules.execution.config.DockerConfig;
import com.bharath.stacked.modules.execution.config.DockerProperties;
import com.bharath.stacked.modules.execution.config.ExecutionProperties;
import com.bharath.stacked.modules.execution.dto.DockerImageDetails;
import com.bharath.stacked.modules.execution.dto.ManagedContainer;
import com.bharath.stacked.modules.execution.mapper.CodeExecutionStatusMapper;
import com.bharath.stacked.modules.execution.mapper.IsolateMetadataParser;
import com.bharath.stacked.modules.execution.registry.ManagedContainerRegistry;
import com.bharath.stacked.modules.execution.resolver.ExecutionConstraintResolver;
import com.bharath.stacked.modules.execution.service.CodeExecutionService;
import com.bharath.stacked.modules.execution.service.DockerExecutionService;
import com.bharath.stacked.modules.execution.service.IsolateExecutionService;
import com.bharath.stacked.modules.execution.service.impl.CodeExecutionServiceImpl;
import com.bharath.stacked.modules.execution.service.impl.DockerExecutionServiceImpl;
import com.bharath.stacked.modules.execution.service.impl.IsolateExecutionServiceImpl;
import com.bharath.stacked.modules.execution.util.TestcontainersRedisRegistryProvider;
import com.bharath.stacked.modules.execution.validator.ExecutionPathValidator;
import com.bharath.stacked.modules.language.Language;
import com.bharath.stacked.modules.language.factory.impl.LanguageFactoryImpl;
import com.bharath.stacked.modules.language.registry.LanguageRegistry;
import com.bharath.stacked.modules.language.registry.impl.LanguageRegistryImpl;
import com.github.dockerjava.api.DockerClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Base class for all live execution integration tests running against real Docker
 * containers and live Testcontainers Redis.
 */
public abstract class BaseExecutionIT {

    protected static DockerClient dockerClient;
    protected static DockerProperties dockerProperties;
    protected static ExecutionProperties executionProperties;
    protected static ManagedContainerRegistry containerRegistry;
    protected static DockerExecutionService dockerExecutionService;
    protected static IsolateExecutionService isolateExecutionService;
    protected static CodeExecutionStatusMapper statusMapper;
    protected static ExecutionPathValidator pathValidator;
    protected static ExecutionConstraintResolver constraintResolver;
    protected static CodeExecutionService codeExecutionService;
    protected static LanguageRegistry languageRegistry;
    protected static boolean dockerAvailable;

    @BeforeAll
    static void initEnvironment() {
        dockerProperties = new DockerProperties();
        dockerProperties.setEnabled(true);
        dockerProperties.setHost("unix:///var/run/docker.sock");

        executionProperties = new ExecutionProperties();

        try {
            DockerConfig config = new DockerConfig();
            dockerClient = config.dockerClient(dockerProperties);
            dockerClient.pingCmd().exec();

            containerRegistry = TestcontainersRedisRegistryProvider.registry();
            dockerExecutionService = new DockerExecutionServiceImpl(dockerClient, dockerProperties, containerRegistry);
            IsolateMetadataParser metadataParser = new IsolateMetadataParser();
            isolateExecutionService = new IsolateExecutionServiceImpl(dockerExecutionService, metadataParser);
            statusMapper = new CodeExecutionStatusMapper();
            pathValidator = new ExecutionPathValidator(executionProperties);
            constraintResolver = new ExecutionConstraintResolver(executionProperties);

            codeExecutionService = new CodeExecutionServiceImpl(
                    dockerExecutionService,
                    isolateExecutionService,
                    statusMapper,
                    executionProperties,
                    pathValidator,
                    constraintResolver
            );

            languageRegistry = new LanguageRegistryImpl(new LanguageFactoryImpl());
            ((LanguageRegistryImpl) languageRegistry).init();

            dockerAvailable = true;
        } catch (Exception e) {
            dockerAvailable = false;
        }
    }

    @AfterEach
    void verifyNoLingeringContainers() {
        if (!dockerAvailable || containerRegistry == null) {
            return;
        }
        List<ManagedContainer> lingering =
                containerRegistry.findCreatedBefore(Instant.now().plusSeconds(60));
        assertThat(lingering)
                .as("No execution containers should linger in Redis after execution completes")
                .isEmpty();
    }

    protected void assumeLanguageImageAvailable(Language language) {
        Assumptions.assumeTrue(dockerAvailable, "Docker daemon is not available; skipping live integration tests.");
        DockerImageDetails img = language.dockerImageDetails();
        boolean exists = dockerExecutionService.isImageExists(img);
        Assumptions.assumeTrue(exists, "Docker image [" + img.reference() + "] not built locally. Skipping.");
    }
}

