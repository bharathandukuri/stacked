package com.bharath.stacked.modules.execution.service.impl;

import com.bharath.stacked.modules.execution.config.DockerConfig;
import com.bharath.stacked.modules.execution.config.DockerProperties;
import com.bharath.stacked.modules.execution.model.DockerContainerDetails;
import com.bharath.stacked.modules.execution.model.DockerExecutionResult;
import com.bharath.stacked.modules.execution.model.DockerImageDetails;
import com.bharath.stacked.modules.execution.service.DockerExecutionService;
import com.github.dockerjava.api.DockerClient;
import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DockerExecutionService Live Integration Tests")
class DockerExecutionServiceIntegrationTest {

    private static DockerClient dockerClient;
    private static DockerProperties dockerProperties;
    private static DockerExecutionService executionService;
    private static boolean dockerAvailable;

    // Use the isolate image which contains bash and isolate
    private final DockerImageDetails testImage =
            com.bharath.stacked.modules.execution.registry.DockerImageRegistry.ISOLATE_1_0.dockerImage();

    private final List<String> containersToCleanup = new ArrayList<>();

    @BeforeAll
    static void initDocker() {
        dockerProperties = new DockerProperties();
        dockerProperties.setEnabled(true);
        dockerProperties.setHost("unix:///var/run/docker.sock");

        try {
            DockerConfig config = new DockerConfig();
            dockerClient = config.dockerClient(dockerProperties);
            dockerClient.pingCmd().exec();
            executionService = new DockerExecutionServiceImpl(dockerClient, dockerProperties);
            dockerAvailable = true;
        } catch (Exception e) {
            dockerAvailable = false;
        }
    }

    @BeforeEach
    void verifyDockerAvailable() {
        Assumptions.assumeTrue(dockerAvailable, "Docker daemon is not available; skipping live integration tests.");
        Assumptions.assumeTrue(executionService.isImageExists(testImage),
                "Required test image [" + testImage.reference() + "] is not available in local Docker daemon.");
    }

    @AfterEach
    void cleanupContainers() {
        if (!dockerAvailable) {
            return;
        }
        for (String containerId : containersToCleanup) {
            try {
                executionService.deleteContainer(containerId);
            } catch (Exception ignored) {
            }
        }
        containersToCleanup.clear();
    }

    @Test
    @DisplayName("isImageExists returns true for locally available image and false for nonexistent image")
    void isImageExistsLive() {
        assertThat(executionService.isImageExists(testImage)).isTrue();

        DockerImageDetails nonexistent = new DockerImageDetails("nonexistent-stacked-image", "99.99", "none");
        assertThat(executionService.isImageExists(nonexistent)).isFalse();
    }

    @Test
    @DisplayName("Complete container lifecycle: create, verify, start, exec, stop, and delete")
    void containerLifecycleAndExecLive() {
        // 1. Create container
        DockerContainerDetails container = executionService.createContainer(testImage);
        assertThat(container).isNotNull();
        assertThat(container.id()).isNotBlank();
        assertThat(container.name()).startsWith("stacked-execution-");
        containersToCleanup.add(container.id());

        // 2. Verify container exists
        assertThat(executionService.isContainerExists(container.id())).isTrue();

        // 3. Start container
        executionService.startContainer(container.id());

        // 4. Execute standard command (echo)
        DockerExecutionResult echoResult = executionService.execContainer(
                container.id(),
                List.of("echo", "hello-stacked-judge")
        );
        assertThat(echoResult).isNotNull();
        assertThat(echoResult.exitCode()).isEqualTo(0L);
        assertThat(echoResult.stdout().trim()).isEqualTo("hello-stacked-judge");
        assertThat(echoResult.stderr()).isEmpty();

        // 5. Execute command writing to stderr
        DockerExecutionResult stderrResult = executionService.execContainer(
                container.id(),
                List.of("sh", "-c", "echo 'judge-stderr-output' >&2")
        );
        assertThat(stderrResult.exitCode()).isEqualTo(0L);
        assertThat(stderrResult.stderr().trim()).isEqualTo("judge-stderr-output");

        // 6. Execute command with non-zero exit code
        DockerExecutionResult failResult = executionService.execContainer(
                container.id(),
                List.of("sh", "-c", "exit 42")
        );
        assertThat(failResult.exitCode()).isEqualTo(42L);

        // 7. Stop container
        executionService.stopContainer(container.id());

        // 8. Delete container
        executionService.deleteContainer(container.id());
        containersToCleanup.remove(container.id());

        // 9. Verify container no longer exists
        assertThat(executionService.isContainerExists(container.id())).isFalse();
    }

    @Test
    @DisplayName("Write file with Base64 encoding and read back file contents in running container")
    void writeAndReadFileLive() {
        DockerContainerDetails container = executionService.createContainer(testImage);
        containersToCleanup.add(container.id());
        executionService.startContainer(container.id());

        String targetPath = "/tmp/test_file.txt";
        String payload = "System.out.println(\"Hello Stacked Judge\");\nLine 2 payload with special chars: ' \" $";

        // 1. Write file
        executionService.writeFile(container.id(), targetPath, payload);

        // 2. Read file back
        String readBack = executionService.readFile(container.id(), targetPath);
        assertThat(readBack).isEqualTo(payload);

        // 3. Reading non-existent file throws DockerExecutionException
        org.junit.jupiter.api.Assertions.assertThrows(
                com.bharath.stacked.modules.execution.exception.DockerExecutionException.class,
                () -> executionService.readFile(container.id(), "/nonexistent/path/never_created.txt")
        );
    }
}
