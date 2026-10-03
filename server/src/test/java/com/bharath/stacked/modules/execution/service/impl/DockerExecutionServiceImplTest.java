package com.bharath.stacked.modules.execution.service.impl;

import com.bharath.stacked.modules.execution.model.DockerContainerDetails;
import com.bharath.stacked.modules.execution.config.DockerProperties;
import com.bharath.stacked.modules.execution.exception.DockerContainerCreationException;
import com.bharath.stacked.modules.execution.exception.DockerContainerDeletionException;
import com.bharath.stacked.modules.execution.exception.DockerImageCreationException;
import com.bharath.stacked.modules.execution.model.DockerImageDetails;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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

    @Test
    @DisplayName("checkImageExists returns true when inspectImageCmd succeeds")
    void checkImageExistsSuccess() {
        DockerImageDetails details = new DockerImageDetails("stacked/isolate", "1.0", "docker/isolate-1_0");
        InspectImageCmd cmd = mock(InspectImageCmd.class);

        when(dockerClient.inspectImageCmd("stacked/isolate:1.0")).thenReturn(cmd);
        when(cmd.exec()).thenReturn(mock(InspectImageResponse.class));

        boolean exists = service.checkImageExists(details);

        assertThat(exists).isTrue();
        verify(dockerClient).inspectImageCmd("stacked/isolate:1.0");
    }

    @Test
    @DisplayName("checkImageExists returns false when inspectImageCmd throws NotFoundException")
    void checkImageExistsNotFound() {
        DockerImageDetails details = new DockerImageDetails("stacked/isolate", "1.0", "docker/isolate-1_0");
        InspectImageCmd cmd = mock(InspectImageCmd.class);

        when(dockerClient.inspectImageCmd("stacked/isolate:1.0")).thenReturn(cmd);
        when(cmd.exec()).thenThrow(new NotFoundException("Image not found"));

        boolean exists = service.checkImageExists(details);

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
        verify(dockerClient, never()).buildImageCmd(any(java.io.File.class));
    }

    @Test
    @DisplayName("createImage throws DockerImageCreationException when resource path does not exist")
    void createImageMissingContext() {
        DockerImageDetails missingContextDetails = new DockerImageDetails("test", "latest", "nonexistent/path/xyz");

        assertThatThrownBy(() -> service.createImage(missingContextDetails))
                .isInstanceOf(DockerImageCreationException.class)
                .hasMessageContaining("Failed to create Docker image: test:latest");
    }

    @Test
    @DisplayName("createContainer creates container when image exists")
    void createContainerSuccess() {
        DockerImageDetails details = new DockerImageDetails("stacked/isolate", "1.0", "docker/isolate-1_0");

        InspectImageCmd inspectCmd = mock(InspectImageCmd.class);
        when(dockerClient.inspectImageCmd("stacked/isolate:1.0")).thenReturn(inspectCmd);
        when(inspectCmd.exec()).thenReturn(mock(InspectImageResponse.class));

        CreateContainerCmd createCmd = mock(CreateContainerCmd.class);
        CreateContainerResponse createResponse = mock(CreateContainerResponse.class);
        when(createResponse.getId()).thenReturn("container-xyz");

        when(dockerClient.createContainerCmd("stacked/isolate:1.0")).thenReturn(createCmd);
        when(createCmd.exec()).thenReturn(createResponse);

        DockerContainerDetails container = service.createContainer(details);

        assertThat(container).isNotNull();
        assertThat(container.id()).isEqualTo("container-xyz");
        assertThat(container.name()).isEqualTo("container-xyz");
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
        when(createCmd.exec()).thenThrow(new RuntimeException("Docker daemon out of disk"));

        assertThatThrownBy(() -> service.createContainer(details))
                .isInstanceOf(DockerContainerCreationException.class)
                .hasMessageContaining("Failed to create Docker container from image: stacked/isolate:1.0");
    }

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
}
