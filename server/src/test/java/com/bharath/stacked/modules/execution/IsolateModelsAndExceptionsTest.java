package com.bharath.stacked.modules.execution;

import com.bharath.stacked.modules.execution.enums.IsolateExecutionStatus;
import com.bharath.stacked.modules.execution.exception.IsolateCleanupException;
import com.bharath.stacked.modules.execution.exception.IsolateException;
import com.bharath.stacked.modules.execution.exception.IsolateExecutionException;
import com.bharath.stacked.modules.execution.exception.IsolateInitializationException;
import com.bharath.stacked.modules.execution.model.DockerContainerDetails;
import com.bharath.stacked.modules.execution.model.DockerImageDetails;
import com.bharath.stacked.modules.execution.model.IsolateExecutionConstraints;
import com.bharath.stacked.modules.execution.model.IsolateExecutionResult;
import com.bharath.stacked.modules.execution.model.SandBoxDetails;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Isolate Domain Models and Exceptions Unit Tests")
class IsolateModelsAndExceptionsTest {

    @Test
    @DisplayName("IsolateExecutionStatus enum contains all expected statuses and values")
    void isolateExecutionStatusEnum() {
        assertThat(IsolateExecutionStatus.values()).containsExactlyInAnyOrder(
                IsolateExecutionStatus.SUCCESS,
                IsolateExecutionStatus.TIME_LIMIT_EXCEEDED,
                IsolateExecutionStatus.MEMORY_LIMIT_EXCEEDED,
                IsolateExecutionStatus.RUNTIME_ERROR,
                IsolateExecutionStatus.SYSTEM_ERROR
        );

        assertThat(IsolateExecutionStatus.valueOf("SUCCESS")).isEqualTo(IsolateExecutionStatus.SUCCESS);
        assertThat(IsolateExecutionStatus.valueOf("TIME_LIMIT_EXCEEDED")).isEqualTo(IsolateExecutionStatus.TIME_LIMIT_EXCEEDED);
        assertThat(IsolateExecutionStatus.valueOf("MEMORY_LIMIT_EXCEEDED")).isEqualTo(IsolateExecutionStatus.MEMORY_LIMIT_EXCEEDED);
        assertThat(IsolateExecutionStatus.valueOf("RUNTIME_ERROR")).isEqualTo(IsolateExecutionStatus.RUNTIME_ERROR);
        assertThat(IsolateExecutionStatus.valueOf("SYSTEM_ERROR")).isEqualTo(IsolateExecutionStatus.SYSTEM_ERROR);
    }

    @Test
    @DisplayName("SandBoxDetails record constructor, accessors, equals, and toString")
    void sandBoxDetailsRecord() {
        DockerContainerDetails container = new DockerContainerDetails("cnt-123", "container-123");

        SandBoxDetails sandbox1 = new SandBoxDetails(42, container);
        SandBoxDetails sandbox2 = new SandBoxDetails(42, container);

        assertThat(sandbox1.isolateBoxId()).isEqualTo(42);
        assertThat(sandbox1.dockerContainerDetails()).isEqualTo(container);
        assertThat(sandbox1).isEqualTo(sandbox2);
        assertThat(sandbox1.hashCode()).isEqualTo(sandbox2.hashCode());
        assertThat(sandbox1.toString()).contains("42").contains("cnt-123");
    }

    @Test
    @DisplayName("IsolateExecutionConstraints default constructor initializes standard limits")
    void isolateExecutionConstraintsDefaultConstructor() {
        IsolateExecutionConstraints constraints = new IsolateExecutionConstraints();

        assertThat(constraints.cpuTimeSeconds()).isEqualTo(2.0);
        assertThat(constraints.wallTimeSeconds()).isEqualTo(5.0);
        assertThat(constraints.memoryKb()).isEqualTo(262144L);
        assertThat(constraints.processLimit()).isEqualTo(50);
        assertThat(constraints.fileSizeKb()).isEqualTo(10240L);
    }

    @Test
    @DisplayName("IsolateExecutionConstraints custom constructor, accessors, equals, and toString")
    void isolateExecutionConstraintsCustomConstructor() {
        IsolateExecutionConstraints c1 = new IsolateExecutionConstraints(1.0, 3.0, 131072L, 5, 2048L);
        IsolateExecutionConstraints c2 = new IsolateExecutionConstraints(1.0, 3.0, 131072L, 5, 2048L);

        assertThat(c1.cpuTimeSeconds()).isEqualTo(1.0);
        assertThat(c1.wallTimeSeconds()).isEqualTo(3.0);
        assertThat(c1.memoryKb()).isEqualTo(131072L);
        assertThat(c1.processLimit()).isEqualTo(5);
        assertThat(c1.fileSizeKb()).isEqualTo(2048L);
        assertThat(c1).isEqualTo(c2);
        assertThat(c1.hashCode()).isEqualTo(c2.hashCode());
        assertThat(c1.toString()).contains("1.0").contains("131072");
    }

    @Test
    @DisplayName("IsolateExecutionResult record accessors, equals, hashCode, and toString")
    void isolateExecutionResultRecord() {
        IsolateExecutionResult result1 = new IsolateExecutionResult(
                IsolateExecutionStatus.SUCCESS,
                "Hello World",
                "",
                0.015,
                0.020,
                1536L,
                0L,
                0L,
                false,
                12L,
                4L
        );

        IsolateExecutionResult result2 = new IsolateExecutionResult(
                IsolateExecutionStatus.SUCCESS,
                "Hello World",
                "",
                0.015,
                0.020,
                1536L,
                0L,
                0L,
                false,
                12L,
                4L
        );

        assertThat(result1.status()).isEqualTo(IsolateExecutionStatus.SUCCESS);
        assertThat(result1.stdout()).isEqualTo("Hello World");
        assertThat(result1.stderr()).isEmpty();
        assertThat(result1.cpuTimeSeconds()).isEqualTo(0.015);
        assertThat(result1.wallTimeSeconds()).isEqualTo(0.020);
        assertThat(result1.memoryKb()).isEqualTo(1536L);
        assertThat(result1.exitCode()).isEqualTo(0L);
        assertThat(result1.exitSignal()).isEqualTo(0L);
        assertThat(result1.killed()).isFalse();
        assertThat(result1.contextSwitchesVoluntary()).isEqualTo(12L);
        assertThat(result1.contextSwitchesForced()).isEqualTo(4L);

        assertThat(result1).isEqualTo(result2);
        assertThat(result1.hashCode()).isEqualTo(result2.hashCode());
        assertThat(result1.toString()).contains("SUCCESS").contains("Hello World");
    }

    @Test
    @DisplayName("IsolateException constructors set message and cause properly")
    void isolateException() {
        IsolateException ex1 = new IsolateException("Isolate error");
        assertThat(ex1.getMessage()).isEqualTo("Isolate error");
        assertThat(ex1.getCause()).isNull();

        Throwable cause = new RuntimeException("Root cause");
        IsolateException ex2 = new IsolateException("Isolate error with cause", cause);
        assertThat(ex2.getMessage()).isEqualTo("Isolate error with cause");
        assertThat(ex2.getCause()).isSameAs(cause);
    }

    @Test
    @DisplayName("IsolateInitializationException constructors set message and cause properly")
    void isolateInitializationException() {
        IsolateInitializationException ex1 = new IsolateInitializationException("Init failure");
        assertThat(ex1).isInstanceOf(IsolateException.class);
        assertThat(ex1.getMessage()).isEqualTo("Init failure");

        Throwable cause = new RuntimeException("Docker failure");
        IsolateInitializationException ex2 = new IsolateInitializationException("Init failure with cause", cause);
        assertThat(ex2).isInstanceOf(IsolateException.class);
        assertThat(ex2.getMessage()).isEqualTo("Init failure with cause");
        assertThat(ex2.getCause()).isSameAs(cause);
    }

    @Test
    @DisplayName("IsolateCleanupException constructors set message and cause properly")
    void isolateCleanupException() {
        IsolateCleanupException ex1 = new IsolateCleanupException("Cleanup failure");
        assertThat(ex1).isInstanceOf(IsolateException.class);
        assertThat(ex1.getMessage()).isEqualTo("Cleanup failure");

        Throwable cause = new RuntimeException("Cleanup root cause");
        IsolateCleanupException ex2 = new IsolateCleanupException("Cleanup failure with cause", cause);
        assertThat(ex2).isInstanceOf(IsolateException.class);
        assertThat(ex2.getMessage()).isEqualTo("Cleanup failure with cause");
        assertThat(ex2.getCause()).isSameAs(cause);
    }

    @Test
    @DisplayName("IsolateExecutionException constructors set message and cause properly")
    void isolateExecutionException() {
        IsolateExecutionException ex1 = new IsolateExecutionException("Exec failure");
        assertThat(ex1).isInstanceOf(IsolateException.class);
        assertThat(ex1.getMessage()).isEqualTo("Exec failure");

        Throwable cause = new RuntimeException("Exec root cause");
        IsolateExecutionException ex2 = new IsolateExecutionException("Exec failure with cause", cause);
        assertThat(ex2).isInstanceOf(IsolateException.class);
        assertThat(ex2.getMessage()).isEqualTo("Exec failure with cause");
        assertThat(ex2.getCause()).isSameAs(cause);
    }
}
