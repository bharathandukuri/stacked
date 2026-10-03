package com.bharath.stacked.modules.execution;

import com.bharath.stacked.modules.execution.config.DockerConfig;
import com.bharath.stacked.modules.execution.config.DockerProperties;
import com.bharath.stacked.modules.execution.exception.*;
import com.bharath.stacked.modules.execution.model.DockerContainerDetails;
import com.bharath.stacked.modules.execution.model.DockerExecutionResult;
import com.bharath.stacked.modules.execution.model.DockerImageDetails;
import com.bharath.stacked.modules.execution.registry.DockerImageRegistry;
import com.github.dockerjava.api.DockerClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Execution Domain Models and Exceptions Unit Tests")
class ExecutionModelsAndExceptionsTest {

    @Test
    @DisplayName("DockerContainerDetails record constructor, getters, equals, and toString")
    void dockerContainerDetailsRecord() {
        DockerContainerDetails container1 = new DockerContainerDetails("cnt-123", "container-123");
        DockerContainerDetails container2 = new DockerContainerDetails("cnt-123", "container-123");

        assertThat(container1.id()).isEqualTo("cnt-123");
        assertThat(container1.name()).isEqualTo("container-123");
        assertThat(container1).isEqualTo(container2);
        assertThat(container1.hashCode()).isEqualTo(container2.hashCode());
        assertThat(container1.toString()).contains("cnt-123").contains("container-123");
    }

    @Test
    @DisplayName("DockerExecutionResult record constructor, getters, equals, and toString")
    void dockerExecutionResultRecord() {
        DockerExecutionResult result1 = new DockerExecutionResult(0L, "Hello stdout", "No stderr");
        DockerExecutionResult result2 = new DockerExecutionResult(0L, "Hello stdout", "No stderr");

        assertThat(result1.exitCode()).isEqualTo(0L);
        assertThat(result1.stdout()).isEqualTo("Hello stdout");
        assertThat(result1.stderr()).isEqualTo("No stderr");
        assertThat(result1).isEqualTo(result2);
        assertThat(result1.hashCode()).isEqualTo(result2.hashCode());
        assertThat(result1.toString()).contains("0").contains("Hello stdout");
    }

    @Test
    @DisplayName("DockerImageDetails record, getters, reference() method, equals, and toString")
    void dockerImageDetails() {
        DockerImageDetails details1 = new DockerImageDetails("stacked/isolate", "1.0", "docker/isolate-1_0");
        DockerImageDetails details2 = new DockerImageDetails("stacked/isolate", "1.0", "docker/isolate-1_0");

        assertThat(details1.name()).isEqualTo("stacked/isolate");
        assertThat(details1.tag()).isEqualTo("1.0");
        assertThat(details1.resourcePath()).isEqualTo("docker/isolate-1_0");
        assertThat(details1.reference()).isEqualTo("stacked/isolate:1.0");
        assertThat(details1).isEqualTo(details2);
        assertThat(details1.hashCode()).isEqualTo(details2.hashCode());
        assertThat(details1.toString()).contains("stacked/isolate").contains("1.0");
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
        assertThat(DockerImageRegistry.valueOf("PYTHON_3_12")).isEqualTo(DockerImageRegistry.PYTHON_3_12);
        assertThat(DockerImageRegistry.valueOf("C_17")).isEqualTo(DockerImageRegistry.C_17);
        assertThat(DockerImageRegistry.valueOf("CPP_23")).isEqualTo(DockerImageRegistry.CPP_23);
        assertThat(DockerImageRegistry.valueOf("JAVASCRIPT_NODE_20")).isEqualTo(DockerImageRegistry.JAVASCRIPT_NODE_20);
        assertThat(DockerImageRegistry.valueOf("MYSQL_8_0")).isEqualTo(DockerImageRegistry.MYSQL_8_0);
        assertThat(DockerImageRegistry.valueOf("POSTGRES_16")).isEqualTo(DockerImageRegistry.POSTGRES_16);
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
    @DisplayName("DockerException and full custom exception hierarchy and constructors")
    void dockerExceptions() {
        Throwable cause = new RuntimeException("socket error");

        // Base DockerException
        DockerException dEx1 = new DockerException("base docker err");
        DockerException dEx2 = new DockerException("base docker err", cause);
        assertThat(dEx1.getMessage()).isEqualTo("base docker err");
        assertThat(dEx2.getCause()).isEqualTo(cause);

        // DockerContainerException
        DockerContainerException dcEx1 = new DockerContainerException("container err");
        DockerContainerException dcEx2 = new DockerContainerException("container err", cause);
        assertThat(dcEx1.getMessage()).isEqualTo("container err");
        assertThat(dcEx2.getCause()).isEqualTo(cause);
        assertThat(dcEx1).isInstanceOf(DockerException.class);

        // DockerContainerNotFoundException
        DockerContainerNotFoundException cnf1 = new DockerContainerNotFoundException("container not found");
        DockerContainerNotFoundException cnf2 = new DockerContainerNotFoundException("container not found", cause);
        assertThat(cnf1.getMessage()).isEqualTo("container not found");
        assertThat(cnf2.getCause()).isEqualTo(cause);
        assertThat(cnf1).isInstanceOf(DockerContainerException.class);

        // DockerContainerCreationException
        DockerContainerCreationException cce1 = new DockerContainerCreationException("failed to create container");
        DockerContainerCreationException cce2 = new DockerContainerCreationException("failed to create container", cause);
        assertThat(cce1.getMessage()).isEqualTo("failed to create container");
        assertThat(cce2.getCause()).isEqualTo(cause);
        assertThat(cce1).isInstanceOf(DockerContainerException.class);

        // DockerContainerDeletionException
        DockerContainerDeletionException cde1 = new DockerContainerDeletionException("failed to delete container");
        DockerContainerDeletionException cde2 = new DockerContainerDeletionException("failed to delete container", cause);
        assertThat(cde1.getMessage()).isEqualTo("failed to delete container");
        assertThat(cde2.getCause()).isEqualTo(cause);
        assertThat(cde1).isInstanceOf(DockerContainerException.class);

        // DockerContainerStartException
        DockerContainerStartException cse1 = new DockerContainerStartException("failed to start container");
        DockerContainerStartException cse2 = new DockerContainerStartException("failed to start container", cause);
        assertThat(cse1.getMessage()).isEqualTo("failed to start container");
        assertThat(cse2.getCause()).isEqualTo(cause);
        assertThat(cse1).isInstanceOf(DockerContainerException.class);

        // DockerContainerStopException
        DockerContainerStopException stp1 = new DockerContainerStopException("failed to stop container");
        DockerContainerStopException stp2 = new DockerContainerStopException("failed to stop container", cause);
        assertThat(stp1.getMessage()).isEqualTo("failed to stop container");
        assertThat(stp2.getCause()).isEqualTo(cause);
        assertThat(stp1).isInstanceOf(DockerContainerException.class);

        // DockerExecutionException
        DockerExecutionException exec1 = new DockerExecutionException("execution failed");
        DockerExecutionException exec2 = new DockerExecutionException("execution failed", cause);
        assertThat(exec1.getMessage()).isEqualTo("execution failed");
        assertThat(exec2.getCause()).isEqualTo(cause);
        assertThat(exec1).isInstanceOf(DockerContainerException.class);

        // DockerImageCreationException
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
