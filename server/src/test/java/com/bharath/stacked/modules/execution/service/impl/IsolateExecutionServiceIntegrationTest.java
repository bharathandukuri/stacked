package com.bharath.stacked.modules.execution.service.impl;

import com.bharath.stacked.modules.execution.config.DockerConfig;
import com.bharath.stacked.modules.execution.config.DockerProperties;
import com.bharath.stacked.modules.execution.enums.IsolateExecutionStatus;
import com.bharath.stacked.modules.execution.dto.DockerContainerDetails;
import com.bharath.stacked.modules.execution.dto.DockerImageDetails;
import com.bharath.stacked.modules.execution.dto.IsolateExecutionConstraints;
import com.bharath.stacked.modules.execution.dto.response.IsolateExecutionResult;
import com.bharath.stacked.modules.execution.dto.IsolateSandBoxDetails;
import com.bharath.stacked.modules.execution.mapper.IsolateMetadataParser;
import com.bharath.stacked.modules.execution.registry.DockerImageRegistry;
import com.bharath.stacked.modules.execution.service.DockerExecutionService;
import com.bharath.stacked.modules.execution.service.IsolateExecutionService;
import com.github.dockerjava.api.DockerClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("IsolateExecutionService Live Integration Tests")
class IsolateExecutionServiceIntegrationTest {

    private static DockerClient dockerClient;
    private static DockerProperties dockerProperties;
    private static DockerExecutionService dockerExecutionService;
    private static IsolateMetadataParser metadataParser;
    private static IsolateExecutionService isolateExecutionService;
    private static boolean dockerAvailable;

    private final DockerImageDetails testImage = DockerImageRegistry.ISOLATE_1_0.dockerImage();
    private final List<IsolateSandBoxDetails> sandboxesToCleanup = new ArrayList<>();
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
            dockerExecutionService = new DockerExecutionServiceImpl(dockerClient, dockerProperties);
            metadataParser = new IsolateMetadataParser();
            isolateExecutionService = new IsolateExecutionServiceImpl(dockerExecutionService, metadataParser);
            dockerAvailable = true;
        } catch (Exception e) {
            dockerAvailable = false;
        }
    }

    @BeforeEach
    void verifyDockerAvailable() {
        Assumptions.assumeTrue(dockerAvailable, "Docker daemon is not available; skipping live integration tests.");
        Assumptions.assumeTrue(dockerExecutionService.isImageExists(testImage),
                "Required test image [" + testImage.reference() + "] is not available in local Docker daemon.");
    }

    @AfterEach
    void cleanupSandboxesAndContainers() {
        for (IsolateSandBoxDetails sandbox : sandboxesToCleanup) {
            try {
                isolateExecutionService.cleanup(sandbox);
            } catch (Exception ignored) {
            }
        }
        sandboxesToCleanup.clear();

        for (String containerId : containersToCleanup) {
            try {
                dockerExecutionService.stopContainer(containerId);
            } catch (Exception ignored) {
            }
            try {
                dockerExecutionService.deleteContainer(containerId);
            } catch (Exception ignored) {
            }
        }
        containersToCleanup.clear();
    }

    private DockerContainerDetails createAndStartContainer(DockerImageDetails image) {
        DockerContainerDetails container = dockerExecutionService.createContainer(image);
        dockerExecutionService.startContainer(container.id());
        containersToCleanup.add(container.id());
        return container;
    }

    @Test
    @DisplayName("Complete Isolate lifecycle: initialize sandbox, execute echo command, verify metrics, and cleanup")
    void executeSimpleEchoCommand() {
        // 1. Caller creates and starts container
        DockerContainerDetails container = createAndStartContainer(testImage);

        // 2. Initialize sandbox inside the running container
        IsolateSandBoxDetails sandbox = isolateExecutionService.initialize(container);
        sandboxesToCleanup.add(sandbox);

        assertThat(sandbox).isNotNull();
        assertThat(sandbox.isolateBoxId()).isBetween(1, 1000);
        assertThat(sandbox.dockerContainerDetails().id()).isEqualTo(container.id());
        assertThat(dockerExecutionService.isContainerExists(container.id())).isTrue();

        // 3. Execute simple command inside Isolate
        List<String> command = List.of("/bin/echo", "Hello Isolate Judge");
        IsolateExecutionConstraints constraints = new IsolateExecutionConstraints();

        IsolateExecutionResult result = isolateExecutionService.executeWithConstraints(
                sandbox,
                command,
                "",
                constraints);

        // 4. Verify results
        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo(IsolateExecutionStatus.SUCCESS);
        assertThat(result.exitCode()).isEqualTo(0L);
        assertThat(result.stdout()).contains("Hello Isolate Judge");
        assertThat(result.stderr()).isEmpty();
        assertThat(result.cpuTimeSeconds()).isNotNull().isGreaterThanOrEqualTo(0.0);
        assertThat(result.wallTimeSeconds()).isNotNull().isGreaterThanOrEqualTo(0.0);
        assertThat(result.memoryKb()).isNotNull().isGreaterThan(0L);

        // 5. Cleanup Isolate box
        isolateExecutionService.cleanup(sandbox);
        sandboxesToCleanup.remove(sandbox);

        // 6. Verify container is still alive (not deleted or stopped by isolate
        // cleanup)
        assertThat(dockerExecutionService.isContainerExists(container.id())).isTrue();
    }

    @Test
    @DisplayName("executeWithConstraints redirects stdin into sandbox and captures matching stdout")
    void executeCatWithStdin() {
        DockerContainerDetails container = createAndStartContainer(testImage);
        IsolateSandBoxDetails sandbox = isolateExecutionService.initialize(container);
        sandboxesToCleanup.add(sandbox);

        String inputPayload = "10 20 30 40\nNext line stdin\n";
        List<String> command = List.of("/bin/cat");
        IsolateExecutionConstraints constraints = new IsolateExecutionConstraints();

        IsolateExecutionResult result = isolateExecutionService.executeWithConstraints(
                sandbox,
                command,
                inputPayload,
                constraints);

        assertThat(result.status()).isEqualTo(IsolateExecutionStatus.SUCCESS);
        assertThat(result.exitCode()).isEqualTo(0L);
        assertThat(result.stdout()).isEqualTo(inputPayload);

        isolateExecutionService.cleanup(sandbox);
        sandboxesToCleanup.remove(sandbox);
        assertThat(dockerExecutionService.isContainerExists(container.id())).isTrue();
    }

    @Test
    @DisplayName("executeWithConstraints identifies non-zero exit code as RUNTIME_ERROR and captures stderr")
    void executeScriptWithNonZeroExitCode() {
        DockerContainerDetails container = createAndStartContainer(testImage);
        IsolateSandBoxDetails sandbox = isolateExecutionService.initialize(container);
        sandboxesToCleanup.add(sandbox);

        List<String> command = List.of("/bin/sh", "-c", "echo 'Critical runtime error' >&2; exit 42");
        IsolateExecutionConstraints constraints = new IsolateExecutionConstraints();

        IsolateExecutionResult result = isolateExecutionService.executeWithConstraints(
                sandbox,
                command,
                "",
                constraints);

        assertThat(result.status()).isEqualTo(IsolateExecutionStatus.RUNTIME_ERROR);
        assertThat(result.exitCode()).isEqualTo(42L);
        assertThat(result.stderr()).contains("Critical runtime error");

        isolateExecutionService.cleanup(sandbox);
        sandboxesToCleanup.remove(sandbox);
        assertThat(dockerExecutionService.isContainerExists(container.id())).isTrue();
    }

    @Test
    @DisplayName("executeWithConstraints identifies timeout as TIME_LIMIT_EXCEEDED")
    void executeCommandExceedingTimeLimit() {
        DockerContainerDetails container = createAndStartContainer(testImage);
        IsolateSandBoxDetails sandbox = isolateExecutionService.initialize(container);
        sandboxesToCleanup.add(sandbox);

        List<String> command = List.of("/bin/sleep", "2");
        // Limit wall time to 0.4 seconds
        IsolateExecutionConstraints constraints = new IsolateExecutionConstraints(
                0.2,
                0.4,
                131072L,
                10,
                10240L);

        IsolateExecutionResult result = isolateExecutionService.executeWithConstraints(
                sandbox,
                command,
                "",
                constraints);

        assertThat(result.status()).isEqualTo(IsolateExecutionStatus.TIME_LIMIT_EXCEEDED);

        isolateExecutionService.cleanup(sandbox);
        sandboxesToCleanup.remove(sandbox);
        assertThat(dockerExecutionService.isContainerExists(container.id())).isTrue();
    }

    @Test
    @DisplayName("Compile and run real Java 21 program inside Isolate sandbox within Docker container")
    void executeJava21ProgramInIsolate() {
        DockerImageDetails javaImage = DockerImageRegistry.JAVA_21.dockerImage();
        Assumptions.assumeTrue(dockerExecutionService.isImageExists(javaImage),
                "Required Java 21 image is not available");

        DockerContainerDetails container = createAndStartContainer(javaImage);
        IsolateSandBoxDetails sandbox = isolateExecutionService.initialize(container);
        sandboxesToCleanup.add(sandbox);

        String boxDir = "/var/lib/isolate/" + sandbox.isolateBoxId() + "/box";
        String javaSource = """
                import java.util.Scanner;
                public class Solution {
                    public static void main(String[] args) {
                        Scanner sc = new Scanner(System.in);
                        int a = sc.nextInt();
                        int b = sc.nextInt();
                        System.out.println("SUM=" + (a + b));
                    }
                }
                """;
        dockerExecutionService.writeFile(sandbox.dockerContainerDetails().id(), boxDir + "/Solution.java", javaSource);

        List<String> command = List.of(
                "/bin/bash",
                "-c",
                "javac Solution.java && java Solution");
        IsolateExecutionConstraints constraints = new IsolateExecutionConstraints(
                5.0,
                10.0,
                null,
                50,
                20480L);

        IsolateExecutionResult result = isolateExecutionService.executeWithConstraints(
                sandbox,
                command,
                "15 27\n",
                constraints);

        assertThat(result.status())
                .withFailMessage("STDERR: [%s], STDOUT: [%s], EXIT: [%s]", result.stderr(), result.stdout(),
                        result.exitCode())
                .isEqualTo(IsolateExecutionStatus.SUCCESS);
        assertThat(result.exitCode()).isEqualTo(0L);
        assertThat(result.stdout()).contains("SUM=42");

        isolateExecutionService.cleanup(sandbox);
        sandboxesToCleanup.remove(sandbox);
        assertThat(dockerExecutionService.isContainerExists(container.id())).isTrue();
    }
}
