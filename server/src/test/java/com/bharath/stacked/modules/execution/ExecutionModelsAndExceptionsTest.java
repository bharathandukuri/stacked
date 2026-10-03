package com.bharath.stacked.modules.execution;

import com.bharath.stacked.modules.execution.config.DockerConfig;
import com.bharath.stacked.modules.execution.config.DockerProperties;
import com.bharath.stacked.modules.execution.exception.DockerContainerCreationException;
import com.bharath.stacked.modules.execution.exception.DockerContainerDeletionException;
import com.bharath.stacked.modules.execution.exception.DockerException;
import com.bharath.stacked.modules.execution.exception.DockerImageCreationException;
import com.bharath.stacked.modules.execution.model.DockerContainerDetails;
import com.bharath.stacked.modules.execution.model.DockerImageDetails;
import com.bharath.stacked.modules.execution.registry.DockerImageRegistry;
import com.github.dockerjava.api.DockerClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Execution Domain Models and Exceptions Unit Tests")
class ExecutionModelsAndExceptionsTest {

    @Test
    @DisplayName("DockerContainer record constructor, getters, and stub lifecycle methods")
    void dockerContainerRecord() {
        DockerContainerDetails container = new DockerContainerDetails("cnt-123", "container-123");

        assertThat(container.id()).isEqualTo("cnt-123");
        assertThat(container.name()).isEqualTo("container-123");
        assertThat(container.toString()).contains("cnt-123");

        // Life-cycle stubs
        container.start();
        container.stop();
    }

    @Test
    @DisplayName("DockerImageDetails record and reference() method")
    void dockerImageDetails() {
        DockerImageDetails details = new DockerImageDetails("stacked/isolate", "1.0", "docker/isolate-1_0");

        assertThat(details.name()).isEqualTo("stacked/isolate");
        assertThat(details.tag()).isEqualTo("1.0");
        assertThat(details.resourcePath()).isEqualTo("docker/isolate-1_0");
        assertThat(details.reference()).isEqualTo("stacked/isolate:1.0");
    }

    @Test
    @DisplayName("DockerImageRegistry enum entries and dockerImage() factory")
    void dockerImageRegistry() {
        for (DockerImageRegistry registry : DockerImageRegistry.values()) {
            DockerImageDetails details = registry.dockerImage();
            assertThat(details).isNotNull();
            assertThat(details.name()).isNotBlank();
            assertThat(details.tag()).isNotBlank();
            assertThat(details.resourcePath()).isNotBlank();
            assertThat(details.reference()).isEqualTo(details.name() + ":" + details.tag());
        }

        assertThat(DockerImageRegistry.valueOf("ISOLATE_1_0")).isEqualTo(DockerImageRegistry.ISOLATE_1_0);
        assertThat(DockerImageRegistry.valueOf("JAVA_21")).isEqualTo(DockerImageRegistry.JAVA_21);
    }

    @Test
    @DisplayName("DockerProperties getters, setters, and defaults")
    void dockerProperties() {
        DockerProperties properties = new DockerProperties();
        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.getHost()).isEqualTo("unix:///var/run/docker.sock");

        properties.setEnabled(false);
        properties.setHost("tcp://localhost:2375");

        assertThat(properties.isEnabled()).isFalse();
        assertThat(properties.getHost()).isEqualTo("tcp://localhost:2375");
    }

    @Test
    @DisplayName("DockerException and custom exception hierarchy and constructors")
    void dockerExceptions() {
        Throwable cause = new RuntimeException("socket error");

        DockerException dEx1 = new DockerException("base docker err");
        DockerException dEx2 = new DockerException("base docker err", cause);
        assertThat(dEx1.getMessage()).isEqualTo("base docker err");
        assertThat(dEx2.getCause()).isEqualTo(cause);

        DockerContainerCreationException cce1 = new DockerContainerCreationException("failed to create container");
        DockerContainerCreationException cce2 = new DockerContainerCreationException("failed to create container", cause);
        assertThat(cce1.getMessage()).isEqualTo("failed to create container");
        assertThat(cce2.getCause()).isEqualTo(cause);
        assertThat(cce1).isInstanceOf(DockerException.class);

        DockerContainerDeletionException cde1 = new DockerContainerDeletionException("failed to delete container");
        DockerContainerDeletionException cde2 = new DockerContainerDeletionException("failed to delete container", cause);
        assertThat(cde1.getMessage()).isEqualTo("failed to delete container");
        assertThat(cde2.getCause()).isEqualTo(cause);
        assertThat(cde1).isInstanceOf(DockerException.class);

        DockerImageCreationException ice1 = new DockerImageCreationException("failed to build image");
        DockerImageCreationException ice2 = new DockerImageCreationException("failed to build image", cause);
        assertThat(ice1.getMessage()).isEqualTo("failed to build image");
        assertThat(ice2.getCause()).isEqualTo(cause);
        assertThat(ice1).isInstanceOf(DockerException.class);
    }

    @Test
    @DisplayName("DockerConfig initializes DockerClient instance from properties")
    void dockerConfigCreatesDockerClient() {
        DockerConfig config = new DockerConfig();
        DockerProperties props = new DockerProperties();
        props.setHost("tcp://127.0.0.1:2375");

        DockerClient client = config.dockerClient(props);
        assertThat(client).isNotNull();

        // Also test with empty host
        props.setHost("");
        DockerClient defaultClient = config.dockerClient(props);
        assertThat(defaultClient).isNotNull();
    }
}
