package com.bharath.stacked.modules.execution.service.impl;

import com.bharath.stacked.modules.execution.config.DockerProperties;
import com.bharath.stacked.modules.execution.dto.DatabaseContainerConstraints;
import com.bharath.stacked.modules.execution.dto.DockerContainerDetails;
import com.bharath.stacked.modules.execution.dto.DockerImageDetails;
import com.bharath.stacked.modules.execution.dto.response.DockerExecutionResult;
import com.bharath.stacked.modules.execution.exception.*;
import com.bharath.stacked.modules.execution.registry.DockerImageRegistry;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.BuildResponseItem;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.ResponseItem;
import com.github.dockerjava.api.model.StreamType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DockerExecutionServiceImpl Unit Tests")
class DockerExecutionServiceImplTest {

    @Mock
    private DockerClient dockerClient;

    private DockerProperties dockerProperties;

    @InjectMocks
    private DockerExecutionServiceImpl service;

    @BeforeEach
    void setUp() {
        dockerProperties = new DockerProperties();
        service = new DockerExecutionServiceImpl(dockerClient, dockerProperties);
    }

    // =========================================================================
    // Image Existence & Validation Tests
    // =========================================================================

    @Test
    @DisplayName("isImageExists returns true when inspectImageCmd succeeds")
    void isImageExistsSuccess() {
        DockerImageDetails details = new DockerImageDetails("stacked/isolate", "1.0", "docker/isolate-1_0");
        InspectImageCmd cmd = mock(InspectImageCmd.class);

        when(dockerClient.inspectImageCmd("stacked/isolate:1.0")).thenReturn(cmd);
        when(cmd.exec()).thenReturn(mock(InspectImageResponse.class));

        boolean exists = service.isImageExists(details);

        assertThat(exists).isTrue();
        verify(dockerClient).inspectImageCmd("stacked/isolate:1.0");
    }

    @Test
    @DisplayName("isImageExists returns false when inspectImageCmd throws NotFoundException")
    void isImageExistsNotFound() {
        DockerImageDetails details = new DockerImageDetails("stacked/isolate", "1.0", "docker/isolate-1_0");
        InspectImageCmd cmd = mock(InspectImageCmd.class);

        when(dockerClient.inspectImageCmd("stacked/isolate:1.0")).thenReturn(cmd);
        when(cmd.exec()).thenThrow(new NotFoundException("Image not found"));

        boolean exists = service.isImageExists(details);

        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("validateImages skips image verification when dockerProperties is disabled")
    void validateImagesDisabled() {
        dockerProperties.setEnabled(false);

        service.validateImages();

        verifyNoInteractions(dockerClient);
    }

    @Test
    @DisplayName("validateImages verifies all images when enabled and images already exist")
    void validateImagesWhenAlreadyPresent() {
        dockerProperties.setEnabled(true);

        InspectImageCmd cmd = mock(InspectImageCmd.class);
        when(dockerClient.inspectImageCmd(anyString())).thenReturn(cmd);
        when(cmd.exec()).thenReturn(mock(InspectImageResponse.class));

        service.validateImages();

        verify(dockerClient, atLeast(2)).inspectImageCmd(anyString());
        verify(dockerClient, never()).buildImageCmd(any(File.class));
    }

    @Test
    @DisplayName("validateImages builds missing images when enabled and image does not exist")
    void validateImagesWhenImagesMissing() {
        dockerProperties.setEnabled(true);

        InspectImageCmd inspectCmd = mock(InspectImageCmd.class);
        when(dockerClient.inspectImageCmd(anyString())).thenReturn(inspectCmd);
        // First check in isImageExists returns false (NotFoundException),
        // Second check after build in createImage returns true
        when(inspectCmd.exec())
                .thenThrow(new NotFoundException("Missing image"))
                .thenReturn(mock(InspectImageResponse.class));

        BuildImageCmd buildCmd = mock(BuildImageCmd.class);
        when(dockerClient.buildImageCmd(any(File.class))).thenReturn(buildCmd);
        when(buildCmd.withTags(any())).thenReturn(buildCmd);

        BuildImageResultCallback mockCallback = mock(BuildImageResultCallback.class);
        when(mockCallback.awaitImageId()).thenReturn("image-id-built");
        when(buildCmd.exec(any())).thenAnswer(invocation -> {
            ResultCallback<?> callback = invocation.getArgument(0);
            if (callback instanceof BuildImageResultCallback birc) {
                birc.onComplete();
            }
            return mockCallback;
        });

        service.validateImages();

        verify(dockerClient, atLeast(1)).buildImageCmd(any(File.class));
    }

    @Test
    @DisplayName("validateImages catches exceptions and logs warning without propagating")
    void validateImagesHandlesExceptionSafely() {
        dockerProperties.setEnabled(true);

        when(dockerClient.inspectImageCmd(anyString())).thenThrow(new RuntimeException("Docker daemon offline"));

        // Should not throw exception
        service.validateImages();
    }

    // =========================================================================
    // Image Creation Tests
    // =========================================================================

    @Test
    @DisplayName("createImage throws DockerImageCreationException when resource path does not exist")
    void createImageMissingContext() {
        DockerImageDetails missingContextDetails = new DockerImageDetails("test", "latest", "nonexistent/path/xyz");

        assertThatThrownBy(() -> service.createImage(missingContextDetails))
                .isInstanceOf(DockerImageCreationException.class)
                .hasMessageContaining("Failed to create Docker image: test:latest")
                .hasRootCauseInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("createImage throws DockerImageCreationException when resource path is not a directory")
    void createImageContextNotDirectory() {
        // application.yaml exists on classpath as a file, not a directory
        DockerImageDetails fileContextDetails = new DockerImageDetails("test", "latest", "application.yaml");

        assertThatThrownBy(() -> service.createImage(fileContextDetails))
                .isInstanceOf(DockerImageCreationException.class)
                .hasMessageContaining("Failed to create Docker image: test:latest")
                .hasRootCauseInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("createImage builds image successfully when context is valid and post-check passes")
    void createImageSuccess() {
        DockerImageDetails details = new DockerImageDetails("execution/isolate", "1.0", "docker/isolate-1_0");

        BuildImageCmd buildCmd = mock(BuildImageCmd.class);
        when(dockerClient.buildImageCmd(any(File.class))).thenReturn(buildCmd);
        when(buildCmd.withTags(any())).thenReturn(buildCmd);

        BuildImageResultCallback mockCallback = mock(BuildImageResultCallback.class);
        when(mockCallback.awaitImageId()).thenReturn("image-id-123");

        when(buildCmd.exec(any())).thenAnswer(invocation -> {
            ResultCallback<?> callback = invocation.getArgument(0);
            if (callback instanceof BuildImageResultCallback birc) {
                BuildResponseItem itemWithStream = mock(BuildResponseItem.class);
                when(itemWithStream.getStream()).thenReturn("Successfully built image\n");
                birc.onNext(itemWithStream);

                BuildResponseItem itemWithError = mock(BuildResponseItem.class);
                ResponseItem.ErrorDetail errorDetail = mock(ResponseItem.ErrorDetail.class);
                when(errorDetail.getMessage()).thenReturn("Build warning");
                when(itemWithError.getErrorDetail()).thenReturn(errorDetail);
                birc.onNext(itemWithError);

                birc.onComplete();
            }
            return mockCallback;
        });

        // After build, isImageExists must return true
        InspectImageCmd inspectCmd = mock(InspectImageCmd.class);
        when(dockerClient.inspectImageCmd("execution/isolate:1.0")).thenReturn(inspectCmd);
        when(inspectCmd.exec()).thenReturn(mock(InspectImageResponse.class));

        service.createImage(details);

        verify(dockerClient).buildImageCmd(any(File.class));
        verify(buildCmd).withTags(any());
        verify(mockCallback).awaitImageId();
    }

    @Test
    @DisplayName("createImage throws DockerImageCreationException when image is still missing after build")
    void createImageFailsPostVerification() {
        DockerImageDetails details = new DockerImageDetails("execution/isolate", "1.0", "docker/isolate-1_0");

        BuildImageCmd buildCmd = mock(BuildImageCmd.class);
        when(dockerClient.buildImageCmd(any(File.class))).thenReturn(buildCmd);
        when(buildCmd.withTags(any())).thenReturn(buildCmd);

        BuildImageResultCallback mockCallback = mock(BuildImageResultCallback.class);
        when(mockCallback.awaitImageId()).thenReturn("image-id-123");
        when(buildCmd.exec(any())).thenAnswer(invocation -> {
            ResultCallback<?> callback = invocation.getArgument(0);
            if (callback instanceof BuildImageResultCallback birc) {
                birc.onComplete();
            }
            return mockCallback;
        });

        // After build, isImageExists returns false
        InspectImageCmd inspectCmd = mock(InspectImageCmd.class);
        when(dockerClient.inspectImageCmd("execution/isolate:1.0")).thenReturn(inspectCmd);
        when(inspectCmd.exec()).thenThrow(new NotFoundException("Still not found"));

        assertThatThrownBy(() -> service.createImage(details))
                .isInstanceOf(DockerImageCreationException.class)
                .hasMessageContaining("Failed to create Docker image: execution/isolate:1.0")
                .hasRootCauseInstanceOf(IllegalStateException.class);
    }

    // =========================================================================
    // Container Creation Tests
    // =========================================================================

    @Test
    @DisplayName("createContainer creates container with unique name when image exists")
    void createContainerSuccess() {
        DockerImageDetails details = new DockerImageDetails("stacked/isolate", "1.0", "docker/isolate-1_0");

        InspectImageCmd inspectCmd = mock(InspectImageCmd.class);
        when(dockerClient.inspectImageCmd("stacked/isolate:1.0")).thenReturn(inspectCmd);
        when(inspectCmd.exec()).thenReturn(mock(InspectImageResponse.class));

        CreateContainerCmd createCmd = mock(CreateContainerCmd.class);
        CreateContainerResponse createResponse = mock(CreateContainerResponse.class);
        when(createResponse.getId()).thenReturn("container-xyz");

        when(dockerClient.createContainerCmd("stacked/isolate:1.0")).thenReturn(createCmd);
        when(createCmd.withName(anyString())).thenReturn(createCmd);
        when(createCmd.withTty(anyBoolean())).thenReturn(createCmd);
        when(createCmd.withHostConfig(any(HostConfig.class))).thenReturn(createCmd);
        when(createCmd.exec()).thenReturn(createResponse);

        DockerContainerDetails container = service.createContainer(details);

        assertThat(container).isNotNull();
        assertThat(container.id()).isEqualTo("container-xyz");
        assertThat(container.name()).startsWith("stacked-execution-");

        ArgumentCaptor<String> nameCaptor = ArgumentCaptor.forClass(String.class);
        verify(createCmd).withName(nameCaptor.capture());
        assertThat(nameCaptor.getValue()).startsWith("stacked-execution-");
        verify(createCmd).withTty(true);
        ArgumentCaptor<HostConfig> hostConfigCaptor = ArgumentCaptor.forClass(HostConfig.class);
        verify(createCmd).withHostConfig(hostConfigCaptor.capture());
        assertThat(hostConfigCaptor.getValue().getPrivileged()).isTrue();
    }

    @Test
    @DisplayName("createContainer throws DockerContainerCreationException when image does not exist")
    void createContainerImageNotFound() {
        DockerImageDetails details = new DockerImageDetails("stacked/isolate", "1.0", "docker/isolate-1_0");

        InspectImageCmd inspectCmd = mock(InspectImageCmd.class);
        when(dockerClient.inspectImageCmd("stacked/isolate:1.0")).thenReturn(inspectCmd);
        when(inspectCmd.exec()).thenThrow(new NotFoundException("No such image"));

        assertThatThrownBy(() -> service.createContainer(details))
                .isInstanceOf(DockerContainerCreationException.class)
                .hasMessageContaining("Docker image does not exist: stacked/isolate:1.0");
    }

    @Test
    @DisplayName("createContainer wraps client exceptions into DockerContainerCreationException")
    void createContainerClientFailure() {
        DockerImageDetails details = new DockerImageDetails("stacked/isolate", "1.0", "docker/isolate-1_0");

        InspectImageCmd inspectCmd = mock(InspectImageCmd.class);
        when(dockerClient.inspectImageCmd("stacked/isolate:1.0")).thenReturn(inspectCmd);
        when(inspectCmd.exec()).thenReturn(mock(InspectImageResponse.class));

        CreateContainerCmd createCmd = mock(CreateContainerCmd.class);
        when(dockerClient.createContainerCmd("stacked/isolate:1.0")).thenReturn(createCmd);
        when(createCmd.withName(anyString())).thenReturn(createCmd);
        when(createCmd.withTty(anyBoolean())).thenReturn(createCmd);
        when(createCmd.withHostConfig(any(HostConfig.class))).thenReturn(createCmd);
        when(createCmd.exec()).thenThrow(new RuntimeException("Docker daemon out of disk"));

        assertThatThrownBy(() -> service.createContainer(details))
                .isInstanceOf(DockerContainerCreationException.class)
                .hasMessageContaining("Failed to create Docker container from image: stacked/isolate:1.0");
    }

    @Test
    @DisplayName("createContainer with DatabaseContainerConstraints sets CPU, memory, PIDs, and networkDisabled")
    void createContainerWithConstraints() {
        DockerImageDetails details = new DockerImageDetails("stacked/mysql", "8.0", "docker/mysql-8_0");

        InspectImageCmd inspectCmd = mock(InspectImageCmd.class);
        when(dockerClient.inspectImageCmd("stacked/mysql:8.0")).thenReturn(inspectCmd);
        when(inspectCmd.exec()).thenReturn(mock(InspectImageResponse.class));

        CreateContainerCmd createCmd = mock(CreateContainerCmd.class);
        CreateContainerResponse createResponse = mock(CreateContainerResponse.class);
        when(createResponse.getId()).thenReturn("container-db-123");

        when(dockerClient.createContainerCmd("stacked/mysql:8.0")).thenReturn(createCmd);
        when(createCmd.withName(anyString())).thenReturn(createCmd);
        when(createCmd.withTty(anyBoolean())).thenReturn(createCmd);
        when(createCmd.withHostConfig(any(HostConfig.class))).thenReturn(createCmd);
        when(createCmd.withNetworkDisabled(true)).thenReturn(createCmd);
        when(createCmd.exec()).thenReturn(createResponse);

        DatabaseContainerConstraints constraints = DatabaseContainerConstraints.builder()
                .cpuLimit(2L)
                .memoryLimitKb(524288L)
                .pidsLimit(150L)
                .networkDisabled(true)
                .build();

        DockerContainerDetails container = service.createContainer(details, constraints);

        assertThat(container).isNotNull();
        assertThat(container.id()).isEqualTo("container-db-123");

        ArgumentCaptor<HostConfig> hostConfigCaptor = ArgumentCaptor.forClass(HostConfig.class);
        verify(createCmd).withHostConfig(hostConfigCaptor.capture());
        HostConfig hostConfig = hostConfigCaptor.getValue();
        assertThat(hostConfig.getPrivileged()).isTrue();
        assertThat(hostConfig.getNanoCPUs()).isEqualTo(2_000_000_000L);
        assertThat(hostConfig.getMemory()).isEqualTo(524288L * 1024L);
        assertThat(hostConfig.getPidsLimit()).isEqualTo(150L);
        assertThat(hostConfig.getNetworkMode()).isEqualTo("none");
        verify(createCmd).withNetworkDisabled(true);
    }

    @Test
    @DisplayName("createContainer default overload with registry and DatabaseContainerConstraints works")
    void createContainerRegistryWithConstraints() {
        InspectImageCmd inspectCmd = mock(InspectImageCmd.class);
        when(dockerClient.inspectImageCmd("execution/mysql:8.0")).thenReturn(inspectCmd);
        when(inspectCmd.exec()).thenReturn(mock(InspectImageResponse.class));

        CreateContainerCmd createCmd = mock(CreateContainerCmd.class);
        CreateContainerResponse createResponse = mock(CreateContainerResponse.class);
        when(createResponse.getId()).thenReturn("container-reg-db");

        when(dockerClient.createContainerCmd("execution/mysql:8.0")).thenReturn(createCmd);
        when(createCmd.withName(anyString())).thenReturn(createCmd);
        when(createCmd.withTty(anyBoolean())).thenReturn(createCmd);
        when(createCmd.withHostConfig(any(HostConfig.class))).thenReturn(createCmd);
        when(createCmd.withNetworkDisabled(true)).thenReturn(createCmd);
        when(createCmd.exec()).thenReturn(createResponse);

        DatabaseContainerConstraints constraints = DatabaseContainerConstraints.defaults();
        DockerContainerDetails container = service.createContainer(DockerImageRegistry.MYSQL_8_0, constraints);

        assertThat(container.id()).isEqualTo("container-reg-db");

        assertThatThrownBy(() -> service.createContainer((DockerImageRegistry) null, constraints))
                .isInstanceOf(DockerContainerCreationException.class)
                .hasMessageContaining("DockerImageRegistry must not be null.");
    }

    // =========================================================================
    // Container Existence, Start & Stop Tests
    // =========================================================================

    @Test
    @DisplayName("isContainerExists returns true when container is inspected successfully")
    void isContainerExistsTrue() {
        InspectContainerCmd cmd = mock(InspectContainerCmd.class);
        when(dockerClient.inspectContainerCmd("cnt-001")).thenReturn(cmd);
        when(cmd.exec()).thenReturn(mock(InspectContainerResponse.class));

        boolean exists = service.isContainerExists("cnt-001");

        assertThat(exists).isTrue();
        verify(dockerClient).inspectContainerCmd("cnt-001");
    }

    @Test
    @DisplayName("isContainerExists returns false when NotFoundException is thrown")
    void isContainerExistsFalse() {
        InspectContainerCmd cmd = mock(InspectContainerCmd.class);
        when(dockerClient.inspectContainerCmd("nonexistent")).thenReturn(cmd);
        when(cmd.exec()).thenThrow(new NotFoundException("Container not found"));

        boolean exists = service.isContainerExists("nonexistent");

        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("startContainer successfully starts existing container")
    void startContainerSuccess() {
        InspectContainerCmd inspectCmd = mock(InspectContainerCmd.class);
        when(dockerClient.inspectContainerCmd("cnt-start")).thenReturn(inspectCmd);
        when(inspectCmd.exec()).thenReturn(mock(InspectContainerResponse.class));

        StartContainerCmd startCmd = mock(StartContainerCmd.class);
        when(dockerClient.startContainerCmd("cnt-start")).thenReturn(startCmd);

        service.startContainer("cnt-start");

        verify(dockerClient).startContainerCmd("cnt-start");
        verify(startCmd).exec();
    }

    @Test
    @DisplayName("startContainer throws DockerContainerNotFoundException when container does not exist")
    void startContainerNotFound() {
        InspectContainerCmd inspectCmd = mock(InspectContainerCmd.class);
        when(dockerClient.inspectContainerCmd("missing-cnt")).thenReturn(inspectCmd);
        when(inspectCmd.exec()).thenThrow(new NotFoundException("No such container"));

        assertThatThrownBy(() -> service.startContainer("missing-cnt"))
                .isInstanceOf(DockerContainerNotFoundException.class)
                .hasMessageContaining("Docker container does not exist: missing-cnt");
    }

    @Test
    @DisplayName("startContainer wraps execution exceptions into DockerContainerStartException")
    void startContainerFailure() {
        InspectContainerCmd inspectCmd = mock(InspectContainerCmd.class);
        when(dockerClient.inspectContainerCmd("cnt-fail")).thenReturn(inspectCmd);
        when(inspectCmd.exec()).thenReturn(mock(InspectContainerResponse.class));

        StartContainerCmd startCmd = mock(StartContainerCmd.class);
        when(dockerClient.startContainerCmd("cnt-fail")).thenReturn(startCmd);
        when(startCmd.exec()).thenThrow(new RuntimeException("Port conflict"));

        assertThatThrownBy(() -> service.startContainer("cnt-fail"))
                .isInstanceOf(DockerContainerStartException.class)
                .hasMessageContaining("Failed to start Docker container: cnt-fail");
    }

    @Test
    @DisplayName("stopContainer successfully stops existing container")
    void stopContainerSuccess() {
        InspectContainerCmd inspectCmd = mock(InspectContainerCmd.class);
        when(dockerClient.inspectContainerCmd("cnt-stop")).thenReturn(inspectCmd);
        when(inspectCmd.exec()).thenReturn(mock(InspectContainerResponse.class));

        StopContainerCmd stopCmd = mock(StopContainerCmd.class);
        when(dockerClient.stopContainerCmd("cnt-stop")).thenReturn(stopCmd);
        when(stopCmd.withTimeout(anyInt())).thenReturn(stopCmd);

        service.stopContainer("cnt-stop");

        verify(dockerClient).stopContainerCmd("cnt-stop");
        verify(stopCmd).withTimeout(1);
        verify(stopCmd).exec();
    }

    @Test
    @DisplayName("stopContainer throws DockerContainerNotFoundException when container does not exist")
    void stopContainerNotFound() {
        InspectContainerCmd inspectCmd = mock(InspectContainerCmd.class);
        when(dockerClient.inspectContainerCmd("missing-cnt")).thenReturn(inspectCmd);
        when(inspectCmd.exec()).thenThrow(new NotFoundException("No such container"));

        assertThatThrownBy(() -> service.stopContainer("missing-cnt"))
                .isInstanceOf(DockerContainerNotFoundException.class)
                .hasMessageContaining("Docker container does not exist: missing-cnt");
    }

    @Test
    @DisplayName("stopContainer wraps execution exceptions into DockerContainerStopException")
    void stopContainerFailure() {
        InspectContainerCmd inspectCmd = mock(InspectContainerCmd.class);
        when(dockerClient.inspectContainerCmd("cnt-stop-fail")).thenReturn(inspectCmd);
        when(inspectCmd.exec()).thenReturn(mock(InspectContainerResponse.class));

        StopContainerCmd stopCmd = mock(StopContainerCmd.class);
        when(dockerClient.stopContainerCmd("cnt-stop-fail")).thenReturn(stopCmd);
        when(stopCmd.withTimeout(anyInt())).thenReturn(stopCmd);
        when(stopCmd.exec()).thenThrow(new RuntimeException("Timeout stopping container"));

        assertThatThrownBy(() -> service.stopContainer("cnt-stop-fail"))
                .isInstanceOf(DockerContainerStopException.class)
                .hasMessageContaining("Failed to stop Docker container: cnt-stop-fail");
    }

    // =========================================================================
    // Command Execution in Container Tests
    // =========================================================================

    @Test
    @DisplayName("execContainer throws DockerContainerNotFoundException when container is not found")
    void execContainerNotFound() {
        InspectContainerCmd inspectCmd = mock(InspectContainerCmd.class);
        when(dockerClient.inspectContainerCmd("missing-cnt")).thenReturn(inspectCmd);
        when(inspectCmd.exec()).thenThrow(new NotFoundException("No container"));

        assertThatThrownBy(() -> service.execContainer("missing-cnt", List.of("echo", "hello")))
                .isInstanceOf(DockerContainerNotFoundException.class)
                .hasMessageContaining("Docker container does not exist: missing-cnt");
    }

    @Test
    @DisplayName("execContainer executes command and captures STDOUT, STDERR, and RAW streams with exit code")
    void execContainerSuccess() {
        String containerId = "cnt-exec";
        List<String> command = List.of("bash", "-c", "echo hello; echo err >&2");

        // Container exists check
        InspectContainerCmd inspectContainerCmd = mock(InspectContainerCmd.class);
        when(dockerClient.inspectContainerCmd(containerId)).thenReturn(inspectContainerCmd);
        when(inspectContainerCmd.exec()).thenReturn(mock(InspectContainerResponse.class));

        // execCreateCmd setup
        ExecCreateCmd execCreateCmd = mock(ExecCreateCmd.class);
        ExecCreateCmdResponse execCreateResponse = mock(ExecCreateCmdResponse.class);
        when(execCreateResponse.getId()).thenReturn("exec-id-123");

        when(dockerClient.execCreateCmd(containerId)).thenReturn(execCreateCmd);
        when(execCreateCmd.withCmd("bash", "-c", "echo hello; echo err >&2")).thenReturn(execCreateCmd);
        when(execCreateCmd.withAttachStdout(true)).thenReturn(execCreateCmd);
        when(execCreateCmd.withAttachStderr(true)).thenReturn(execCreateCmd);
        when(execCreateCmd.exec()).thenReturn(execCreateResponse);

        // execStartCmd setup
        ExecStartCmd execStartCmd = mock(ExecStartCmd.class);
        when(dockerClient.execStartCmd("exec-id-123")).thenReturn(execStartCmd);
        when(execStartCmd.withDetach(false)).thenReturn(execStartCmd);

        when(execStartCmd.exec(any())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            ResultCallback<Frame> callback = invocation.getArgument(0);

            // Emit STDOUT frame
            callback.onNext(new Frame(StreamType.STDOUT, "Standard output text\n".getBytes(StandardCharsets.UTF_8)));
            // Emit STDERR frame
            callback.onNext(new Frame(StreamType.STDERR, "Standard error text\n".getBytes(StandardCharsets.UTF_8)));
            // Emit RAW frame
            callback.onNext(new Frame(StreamType.RAW, "Raw output text\n".getBytes(StandardCharsets.UTF_8)));

            callback.onComplete();

            return callback;
        });

        // inspectExecCmd setup
        InspectExecCmd inspectExecCmd = mock(InspectExecCmd.class);
        InspectExecResponse inspectExecResponse = mock(InspectExecResponse.class);
        when(inspectExecResponse.getExitCodeLong()).thenReturn(0L);

        when(dockerClient.inspectExecCmd("exec-id-123")).thenReturn(inspectExecCmd);
        when(inspectExecCmd.exec()).thenReturn(inspectExecResponse);

        DockerExecutionResult result = service.execContainer(containerId, command);

        assertThat(result).isNotNull();
        assertThat(result.exitCode()).isEqualTo(0L);
        assertThat(result.stdout()).isEqualTo("Standard output text\nRaw output text\n");
        assertThat(result.stderr()).isEqualTo("Standard error text\n");
    }

    @Test
    @DisplayName("execContainer handles null exit code by defaulting to -1")
    void execContainerNullExitCode() {
        String containerId = "cnt-exec-null";
        List<String> command = List.of("sleep", "1");

        InspectContainerCmd inspectContainerCmd = mock(InspectContainerCmd.class);
        when(dockerClient.inspectContainerCmd(containerId)).thenReturn(inspectContainerCmd);
        when(inspectContainerCmd.exec()).thenReturn(mock(InspectContainerResponse.class));

        ExecCreateCmd execCreateCmd = mock(ExecCreateCmd.class);
        ExecCreateCmdResponse execCreateResponse = mock(ExecCreateCmdResponse.class);
        when(execCreateResponse.getId()).thenReturn("exec-id-null");

        when(dockerClient.execCreateCmd(containerId)).thenReturn(execCreateCmd);
        when(execCreateCmd.withCmd(any(String[].class))).thenReturn(execCreateCmd);
        when(execCreateCmd.withAttachStdout(true)).thenReturn(execCreateCmd);
        when(execCreateCmd.withAttachStderr(true)).thenReturn(execCreateCmd);
        when(execCreateCmd.exec()).thenReturn(execCreateResponse);

        ExecStartCmd execStartCmd = mock(ExecStartCmd.class);
        when(dockerClient.execStartCmd("exec-id-null")).thenReturn(execStartCmd);
        when(execStartCmd.withDetach(false)).thenReturn(execStartCmd);
        when(execStartCmd.exec(any())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            ResultCallback<Frame> callback = invocation.getArgument(0);
            callback.onComplete();
            return callback;
        });

        InspectExecCmd inspectExecCmd = mock(InspectExecCmd.class);
        InspectExecResponse inspectExecResponse = mock(InspectExecResponse.class);
        when(inspectExecResponse.getExitCodeLong()).thenReturn(null);

        when(dockerClient.inspectExecCmd("exec-id-null")).thenReturn(inspectExecCmd);
        when(inspectExecCmd.exec()).thenReturn(inspectExecResponse);

        DockerExecutionResult result = service.execContainer(containerId, command);

        assertThat(result.exitCode()).isEqualTo(-1L);
    }

    @Test
    @DisplayName("execContainer handles InterruptedException and restores thread interrupt flag")
    void execContainerInterrupted() {
        String containerId = "cnt-interrupted";
        List<String> command = List.of("long-running-cmd");

        InspectContainerCmd inspectContainerCmd = mock(InspectContainerCmd.class);
        when(dockerClient.inspectContainerCmd(containerId)).thenReturn(inspectContainerCmd);
        when(inspectContainerCmd.exec()).thenReturn(mock(InspectContainerResponse.class));

        ExecCreateCmd execCreateCmd = mock(ExecCreateCmd.class);
        ExecCreateCmdResponse execCreateResponse = mock(ExecCreateCmdResponse.class);
        when(execCreateResponse.getId()).thenReturn("exec-id-int");

        when(dockerClient.execCreateCmd(containerId)).thenReturn(execCreateCmd);
        when(execCreateCmd.withCmd(any(String[].class))).thenReturn(execCreateCmd);
        when(execCreateCmd.withAttachStdout(true)).thenReturn(execCreateCmd);
        when(execCreateCmd.withAttachStderr(true)).thenReturn(execCreateCmd);
        when(execCreateCmd.exec()).thenReturn(execCreateResponse);

        ExecStartCmd execStartCmd = mock(ExecStartCmd.class);
        when(dockerClient.execStartCmd("exec-id-int")).thenReturn(execStartCmd);
        when(execStartCmd.withDetach(false)).thenReturn(execStartCmd);

        when(execStartCmd.exec(any())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            ResultCallback.Adapter<Frame> cb = invocation.getArgument(0);
            ResultCallback.Adapter<Frame> spyCb = spy(cb);
            doThrow(new InterruptedException("Task timed out")).when(spyCb).awaitCompletion();
            return spyCb;
        });

        assertThatThrownBy(() -> service.execContainer(containerId, command))
                .isInstanceOf(DockerExecutionException.class)
                .hasMessageContaining("Docker command execution was interrupted.")
                .hasRootCauseInstanceOf(InterruptedException.class);

        // Verify thread interrupt flag was set and clear it for clean test runner state
        assertThat(Thread.interrupted()).isTrue();
    }

    @Test
    @DisplayName("execContainer wraps generic execution errors into DockerExecutionException")
    void execContainerGenericFailure() {
        String containerId = "cnt-crash";
        List<String> command = List.of("bad-cmd");

        InspectContainerCmd inspectContainerCmd = mock(InspectContainerCmd.class);
        when(dockerClient.inspectContainerCmd(containerId)).thenReturn(inspectContainerCmd);
        when(inspectContainerCmd.exec()).thenReturn(mock(InspectContainerResponse.class));

        when(dockerClient.execCreateCmd(containerId)).thenThrow(new RuntimeException("Docker daemon IO failure"));

        assertThatThrownBy(() -> service.execContainer(containerId, command))
                .isInstanceOf(DockerExecutionException.class)
                .hasMessageContaining("Failed to execute command in Docker container: cnt-crash")
                .hasRootCauseInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("execContainer with timeLimitMs succeeds within deadline")
    void execContainerWithTimeLimitSuccess() {
        String containerId = "cnt-timed";
        List<String> command = List.of("quick-query");

        InspectContainerCmd inspectContainerCmd = mock(InspectContainerCmd.class);
        when(dockerClient.inspectContainerCmd(containerId)).thenReturn(inspectContainerCmd);
        when(inspectContainerCmd.exec()).thenReturn(mock(InspectContainerResponse.class));

        ExecCreateCmd execCreateCmd = mock(ExecCreateCmd.class);
        ExecCreateCmdResponse execCreateResponse = mock(ExecCreateCmdResponse.class);
        when(execCreateResponse.getId()).thenReturn("exec-id-timed");

        when(dockerClient.execCreateCmd(containerId)).thenReturn(execCreateCmd);
        when(execCreateCmd.withCmd(any(String[].class))).thenReturn(execCreateCmd);
        when(execCreateCmd.withAttachStdout(true)).thenReturn(execCreateCmd);
        when(execCreateCmd.withAttachStderr(true)).thenReturn(execCreateCmd);
        when(execCreateCmd.exec()).thenReturn(execCreateResponse);

        ExecStartCmd execStartCmd = mock(ExecStartCmd.class);
        when(dockerClient.execStartCmd("exec-id-timed")).thenReturn(execStartCmd);
        when(execStartCmd.withDetach(false)).thenReturn(execStartCmd);

        when(execStartCmd.exec(any())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            ResultCallback.Adapter<Frame> cb = invocation.getArgument(0);
            ResultCallback.Adapter<Frame> spyCb = spy(cb);
            doReturn(true).when(spyCb).awaitCompletion(eq(3000L), any());
            return spyCb;
        });

        InspectExecCmd inspectExecCmd = mock(InspectExecCmd.class);
        InspectExecResponse inspectExecResponse = mock(InspectExecResponse.class);
        when(inspectExecResponse.getExitCodeLong()).thenReturn(0L);

        when(dockerClient.inspectExecCmd("exec-id-timed")).thenReturn(inspectExecCmd);
        when(inspectExecCmd.exec()).thenReturn(inspectExecResponse);

        DockerExecutionResult result = service.execContainer(containerId, command, 3000L);

        assertThat(result.exitCode()).isEqualTo(0L);
    }

    @Test
    @DisplayName("execContainer with timeLimitMs returns exitCode 124 when execution times out")
    void execContainerWithTimeLimitTimeout() {
        String containerId = "cnt-timeout";
        List<String> command = List.of("slow-query");

        InspectContainerCmd inspectContainerCmd = mock(InspectContainerCmd.class);
        when(dockerClient.inspectContainerCmd(containerId)).thenReturn(inspectContainerCmd);
        when(inspectContainerCmd.exec()).thenReturn(mock(InspectContainerResponse.class));

        ExecCreateCmd execCreateCmd = mock(ExecCreateCmd.class);
        ExecCreateCmdResponse execCreateResponse = mock(ExecCreateCmdResponse.class);
        when(execCreateResponse.getId()).thenReturn("exec-id-timeout");

        when(dockerClient.execCreateCmd(containerId)).thenReturn(execCreateCmd);
        when(execCreateCmd.withCmd(any(String[].class))).thenReturn(execCreateCmd);
        when(execCreateCmd.withAttachStdout(true)).thenReturn(execCreateCmd);
        when(execCreateCmd.withAttachStderr(true)).thenReturn(execCreateCmd);
        when(execCreateCmd.exec()).thenReturn(execCreateResponse);

        ExecStartCmd execStartCmd = mock(ExecStartCmd.class);
        when(dockerClient.execStartCmd("exec-id-timeout")).thenReturn(execStartCmd);
        when(execStartCmd.withDetach(false)).thenReturn(execStartCmd);

        when(execStartCmd.exec(any())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            ResultCallback.Adapter<Frame> cb = invocation.getArgument(0);
            ResultCallback.Adapter<Frame> spyCb = spy(cb);
            doReturn(false).when(spyCb).awaitCompletion(eq(2000L), any());
            return spyCb;
        });

        DockerExecutionResult result = service.execContainer(containerId, command, 2000L);

        assertThat(result.exitCode()).isEqualTo(124L);
        assertThat(result.stderr()).contains("Command timed out after 2000 ms.");
    }

    // =========================================================================
    // Container Deletion Tests
    // =========================================================================

    @Test
    @DisplayName("deleteContainer successfully removes container with force")
    void deleteContainerSuccess() {
        RemoveContainerCmd removeCmd = mock(RemoveContainerCmd.class);
        when(dockerClient.removeContainerCmd("container-123")).thenReturn(removeCmd);
        when(removeCmd.withForce(true)).thenReturn(removeCmd);

        service.deleteContainer("container-123");

        verify(dockerClient).removeContainerCmd("container-123");
        verify(removeCmd).withForce(true);
        verify(removeCmd).exec();
    }

    @Test
    @DisplayName("deleteContainer swallows NotFoundException when container does not exist")
    void deleteContainerNotFoundHandledCleanly() {
        RemoveContainerCmd removeCmd = mock(RemoveContainerCmd.class);
        when(dockerClient.removeContainerCmd("nonexistent-container")).thenReturn(removeCmd);
        when(removeCmd.withForce(true)).thenReturn(removeCmd);
        doThrow(new NotFoundException("No such container")).when(removeCmd).exec();

        // Should not throw
        service.deleteContainer("nonexistent-container");

        verify(removeCmd).exec();
    }

    @Test
    @DisplayName("deleteContainer throws DockerContainerDeletionException on generic docker client failure")
    void deleteContainerFailure() {
        RemoveContainerCmd removeCmd = mock(RemoveContainerCmd.class);
        when(dockerClient.removeContainerCmd("container-broken")).thenReturn(removeCmd);
        when(removeCmd.withForce(true)).thenReturn(removeCmd);
        doThrow(new RuntimeException("Docker daemon IO failure")).when(removeCmd).exec();

        assertThatThrownBy(() -> service.deleteContainer("container-broken"))
                .isInstanceOf(DockerContainerDeletionException.class)
                .hasMessageContaining("Failed to delete Docker container: container-broken");
    }

    // =========================================================================
    // Read & Write File Tests
    // =========================================================================

    @Test
    @DisplayName("readFile returns file contents when cat command succeeds")
    void readFileSuccess() {
        String containerId = "cnt-read";
        String path = "/tmp/data.txt";

        InspectContainerCmd inspectCmd = mock(InspectContainerCmd.class);
        when(dockerClient.inspectContainerCmd(containerId)).thenReturn(inspectCmd);
        when(inspectCmd.exec()).thenReturn(mock(InspectContainerResponse.class));

        ExecCreateCmd execCreateCmd = mock(ExecCreateCmd.class);
        ExecCreateCmdResponse execCreateResponse = mock(ExecCreateCmdResponse.class);
        when(execCreateResponse.getId()).thenReturn("exec-cat-1");

        when(dockerClient.execCreateCmd(containerId)).thenReturn(execCreateCmd);
        when(execCreateCmd.withCmd("cat", path)).thenReturn(execCreateCmd);
        when(execCreateCmd.withAttachStdout(true)).thenReturn(execCreateCmd);
        when(execCreateCmd.withAttachStderr(true)).thenReturn(execCreateCmd);
        when(execCreateCmd.exec()).thenReturn(execCreateResponse);

        ExecStartCmd execStartCmd = mock(ExecStartCmd.class);
        when(dockerClient.execStartCmd("exec-cat-1")).thenReturn(execStartCmd);
        when(execStartCmd.withDetach(false)).thenReturn(execStartCmd);
        when(execStartCmd.exec(any())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            ResultCallback<Frame> cb = invocation.getArgument(0);
            cb.onNext(new Frame(StreamType.STDOUT, "file content here\n".getBytes(StandardCharsets.UTF_8)));
            cb.onComplete();
            return cb;
        });

        InspectExecCmd inspectExecCmd = mock(InspectExecCmd.class);
        InspectExecResponse inspectExecResponse = mock(InspectExecResponse.class);
        when(inspectExecResponse.getExitCodeLong()).thenReturn(0L);
        when(dockerClient.inspectExecCmd("exec-cat-1")).thenReturn(inspectExecCmd);
        when(inspectExecCmd.exec()).thenReturn(inspectExecResponse);

        String content = service.readFile(containerId, path);

        assertThat(content).isEqualTo("file content here\n");
    }

    @Test
    @DisplayName("readFile throws DockerExecutionException when cat exits with non-zero exit code")
    void readFileFailureNonZeroExit() {
        String containerId = "cnt-read-fail";
        String path = "/missing/file.txt";

        InspectContainerCmd inspectCmd = mock(InspectContainerCmd.class);
        when(dockerClient.inspectContainerCmd(containerId)).thenReturn(inspectCmd);
        when(inspectCmd.exec()).thenReturn(mock(InspectContainerResponse.class));

        ExecCreateCmd execCreateCmd = mock(ExecCreateCmd.class);
        ExecCreateCmdResponse execCreateResponse = mock(ExecCreateCmdResponse.class);
        when(execCreateResponse.getId()).thenReturn("exec-cat-fail");

        when(dockerClient.execCreateCmd(containerId)).thenReturn(execCreateCmd);
        when(execCreateCmd.withCmd("cat", path)).thenReturn(execCreateCmd);
        when(execCreateCmd.withAttachStdout(true)).thenReturn(execCreateCmd);
        when(execCreateCmd.withAttachStderr(true)).thenReturn(execCreateCmd);
        when(execCreateCmd.exec()).thenReturn(execCreateResponse);

        ExecStartCmd execStartCmd = mock(ExecStartCmd.class);
        when(dockerClient.execStartCmd("exec-cat-fail")).thenReturn(execStartCmd);
        when(execStartCmd.withDetach(false)).thenReturn(execStartCmd);
        when(execStartCmd.exec(any())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            ResultCallback<Frame> cb = invocation.getArgument(0);
            cb.onNext(new Frame(StreamType.STDERR,
                    "cat: /missing/file.txt: No such file or directory".getBytes(StandardCharsets.UTF_8)));
            cb.onComplete();
            return cb;
        });

        InspectExecCmd inspectExecCmd = mock(InspectExecCmd.class);
        InspectExecResponse inspectExecResponse = mock(InspectExecResponse.class);
        when(inspectExecResponse.getExitCodeLong()).thenReturn(1L);
        when(dockerClient.inspectExecCmd("exec-cat-fail")).thenReturn(inspectExecCmd);
        when(inspectExecCmd.exec()).thenReturn(inspectExecResponse);

        assertThatThrownBy(() -> service.readFile(containerId, path))
                .isInstanceOf(DockerExecutionException.class)
                .hasMessageContaining("Failed to read file [" + path + "] from container [" + containerId + "]")
                .hasMessageContaining("No such file or directory");
    }

    @Test
    @DisplayName("writeFile encodes payload to Base64 and writes successfully via bash pipe")
    void writeFileSuccess() {
        String containerId = "cnt-write";
        String path = "/workspace/main.cpp";
        String code = "int main() { return 0; }";

        InspectContainerCmd inspectCmd = mock(InspectContainerCmd.class);
        when(dockerClient.inspectContainerCmd(containerId)).thenReturn(inspectCmd);
        when(inspectCmd.exec()).thenReturn(mock(InspectContainerResponse.class));

        ExecCreateCmd execCreateCmd = mock(ExecCreateCmd.class);
        ExecCreateCmdResponse execCreateResponse = mock(ExecCreateCmdResponse.class);
        when(execCreateResponse.getId()).thenReturn("exec-write-1");

        when(dockerClient.execCreateCmd(containerId)).thenReturn(execCreateCmd);
        when(execCreateCmd.withCmd(any(String[].class))).thenReturn(execCreateCmd);
        when(execCreateCmd.withAttachStdout(true)).thenReturn(execCreateCmd);
        when(execCreateCmd.withAttachStderr(true)).thenReturn(execCreateCmd);
        when(execCreateCmd.exec()).thenReturn(execCreateResponse);

        ExecStartCmd execStartCmd = mock(ExecStartCmd.class);
        when(dockerClient.execStartCmd("exec-write-1")).thenReturn(execStartCmd);
        when(execStartCmd.withDetach(false)).thenReturn(execStartCmd);
        when(execStartCmd.exec(any())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            ResultCallback<Frame> cb = invocation.getArgument(0);
            cb.onComplete();
            return cb;
        });

        InspectExecCmd inspectExecCmd = mock(InspectExecCmd.class);
        InspectExecResponse inspectExecResponse = mock(InspectExecResponse.class);
        when(inspectExecResponse.getExitCodeLong()).thenReturn(0L);
        when(dockerClient.inspectExecCmd("exec-write-1")).thenReturn(inspectExecCmd);
        when(inspectExecCmd.exec()).thenReturn(inspectExecResponse);

        service.writeFile(containerId, path, code);

        ArgumentCaptor<String[]> cmdCaptor = ArgumentCaptor.forClass(String[].class);
        verify(execCreateCmd).withCmd(cmdCaptor.capture());
        String[] capturedArgs = cmdCaptor.getValue();
        assertThat(capturedArgs).hasSize(3);
        assertThat(capturedArgs[0]).isEqualTo("bash");
        assertThat(capturedArgs[1]).isEqualTo("-c");
        assertThat(capturedArgs[2]).contains("base64 -d > '" + path + "'");
    }

    @Test
    @DisplayName("writeFile throws DockerExecutionException when base64 write command fails")
    void writeFileFailureNonZeroExit() {
        String containerId = "cnt-write-fail";
        String path = "/root/secret.txt";
        String content = "hello";

        InspectContainerCmd inspectCmd = mock(InspectContainerCmd.class);
        when(dockerClient.inspectContainerCmd(containerId)).thenReturn(inspectCmd);
        when(inspectCmd.exec()).thenReturn(mock(InspectContainerResponse.class));

        ExecCreateCmd execCreateCmd = mock(ExecCreateCmd.class);
        ExecCreateCmdResponse execCreateResponse = mock(ExecCreateCmdResponse.class);
        when(execCreateResponse.getId()).thenReturn("exec-write-fail");

        when(dockerClient.execCreateCmd(containerId)).thenReturn(execCreateCmd);
        when(execCreateCmd.withCmd(any(String[].class))).thenReturn(execCreateCmd);
        when(execCreateCmd.withAttachStdout(true)).thenReturn(execCreateCmd);
        when(execCreateCmd.withAttachStderr(true)).thenReturn(execCreateCmd);
        when(execCreateCmd.exec()).thenReturn(execCreateResponse);

        ExecStartCmd execStartCmd = mock(ExecStartCmd.class);
        when(dockerClient.execStartCmd("exec-write-fail")).thenReturn(execStartCmd);
        when(execStartCmd.withDetach(false)).thenReturn(execStartCmd);
        when(execStartCmd.exec(any())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            ResultCallback<Frame> cb = invocation.getArgument(0);
            cb.onNext(new Frame(StreamType.STDERR,
                    "bash: /root/secret.txt: Permission denied".getBytes(StandardCharsets.UTF_8)));
            cb.onComplete();
            return cb;
        });

        InspectExecCmd inspectExecCmd = mock(InspectExecCmd.class);
        InspectExecResponse inspectExecResponse = mock(InspectExecResponse.class);
        when(inspectExecResponse.getExitCodeLong()).thenReturn(1L);
        when(dockerClient.inspectExecCmd("exec-write-fail")).thenReturn(inspectExecCmd);
        when(inspectExecCmd.exec()).thenReturn(inspectExecResponse);

        assertThatThrownBy(() -> service.writeFile(containerId, path, content))
                .isInstanceOf(DockerExecutionException.class)
                .hasMessageContaining("Failed to write file [" + path + "] to container [" + containerId + "]")
                .hasMessageContaining("Permission denied");
    }
}
