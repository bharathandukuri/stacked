package com.bharath.stacked.modules.execution.service.impl;

import com.bharath.stacked.modules.execution.enums.IsolateExecutionStatus;
import com.bharath.stacked.modules.execution.exception.DockerExecutionException;
import com.bharath.stacked.modules.execution.exception.IsolateCleanupException;
import com.bharath.stacked.modules.execution.exception.IsolateExecutionException;
import com.bharath.stacked.modules.execution.exception.IsolateInitializationException;
import com.bharath.stacked.modules.execution.dto.DockerContainerDetails;
import com.bharath.stacked.modules.execution.dto.response.DockerExecutionResult;
import com.bharath.stacked.modules.execution.dto.DockerImageDetails;
import com.bharath.stacked.modules.execution.dto.IsolateExecutionConstraints;
import com.bharath.stacked.modules.execution.dto.response.IsolateExecutionResult;
import com.bharath.stacked.modules.execution.dto.IsolateSandBoxDetails;
import com.bharath.stacked.modules.execution.service.DockerExecutionService;
import com.bharath.stacked.modules.execution.mapper.IsolateMetadataParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("IsolateExecutionServiceImpl Unit Tests")
class IsolateExecutionServiceImplTest {

    @Mock
    private DockerExecutionService dockerExecutionService;

    @Mock
    private IsolateMetadataParser isolateMetadataParser;

    @InjectMocks
    private IsolateExecutionServiceImpl isolateExecutionService;

    private DockerContainerDetails testContainer;
    private IsolateSandBoxDetails testSandbox;

    @BeforeEach
    void setUp() {
        testContainer = new DockerContainerDetails("cnt-12345", "stacked-execution-uuid");
        testSandbox = new IsolateSandBoxDetails(55, testContainer);
    }

    // =========================================================================
    // Initialization Tests
    // =========================================================================

    @Test
    @DisplayName("initialize runs isolate --init in the provided container successfully")
    void initializeSuccess() {
        when(dockerExecutionService.execContainer(eq("cnt-12345"), anyList()))
                .thenReturn(new DockerExecutionResult(0L, "/var/lib/isolate/55", ""));

        IsolateSandBoxDetails sandboxDetailsIsolate = isolateExecutionService.initialize(testContainer);

        assertThat(sandboxDetailsIsolate).isNotNull();
        assertThat(sandboxDetailsIsolate.dockerContainerDetails()).isEqualTo(testContainer);
        assertThat(sandboxDetailsIsolate.isolateBoxId()).isBetween(1, 1000);

        verify(dockerExecutionService, never()).createContainer(any(DockerImageDetails.class));
        verify(dockerExecutionService, never()).startContainer(anyString());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<String>> cmdCaptor = ArgumentCaptor.forClass(List.class);
        verify(dockerExecutionService).execContainer(eq("cnt-12345"), cmdCaptor.capture());

        List<String> cmd = cmdCaptor.getValue();
        assertThat(cmd.get(0)).isEqualTo("isolate");
        assertThat(cmd.get(1)).isEqualTo("--init");
        assertThat(cmd.get(2)).startsWith("--box-id=");
    }

    @Test
    @DisplayName("initialize throws IsolateInitializationException when container details are null or blank")
    void initializeFailureNullOrBlankContainer() {
        assertThatThrownBy(() -> isolateExecutionService.initialize(null))
                .isInstanceOf(IsolateInitializationException.class)
                .hasMessageContaining("Docker container details must not be null or empty.");

        assertThatThrownBy(() -> isolateExecutionService.initialize(new DockerContainerDetails("", "name")))
                .isInstanceOf(IsolateInitializationException.class)
                .hasMessageContaining("Docker container details must not be null or empty.");
    }

    @Test
    @DisplayName("initialize throws IsolateInitializationException when isolate --init returns non-zero")
    void initializeFailureNonZeroExit() {
        when(dockerExecutionService.execContainer(eq("cnt-12345"), anyList()))
                .thenReturn(new DockerExecutionResult(1L, "", "isolate: failed to create cgroup"));

        assertThatThrownBy(() -> isolateExecutionService.initialize(testContainer))
                .isInstanceOf(IsolateInitializationException.class)
                .hasMessageContaining("Failed to initialize Isolate box")
                .hasMessageContaining("isolate: failed to create cgroup");
    }

    @Test
    @DisplayName("initialize wraps DockerExecutionException into IsolateInitializationException")
    void initializeFailureDockerException() {
        when(dockerExecutionService.execContainer(eq("cnt-12345"), anyList()))
                .thenThrow(new RuntimeException("Docker daemon connection lost"));

        assertThatThrownBy(() -> isolateExecutionService.initialize(testContainer))
                .isInstanceOf(IsolateInitializationException.class)
                .hasMessageContaining("Failed to initialize Isolate sandbox in container [cnt-12345].")
                .hasCauseInstanceOf(RuntimeException.class);
    }

    // =========================================================================
    // Cleanup Tests
    // =========================================================================

    @Test
    @DisplayName("cleanup runs isolate --cleanup without stopping or deleting container")
    void cleanupSuccess() {
        when(dockerExecutionService.execContainer(eq("cnt-12345"), anyList()))
                .thenReturn(new DockerExecutionResult(0L, "", ""));

        isolateExecutionService.cleanup(testSandbox);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<String>> cmdCaptor = ArgumentCaptor.forClass(List.class);
        verify(dockerExecutionService).execContainer(eq("cnt-12345"), cmdCaptor.capture());

        List<String> cmd = cmdCaptor.getValue();
        assertThat(cmd).containsExactly("isolate", "--cleanup", "--box-id=55");

        verify(dockerExecutionService, never()).stopContainer(anyString());
        verify(dockerExecutionService, never()).deleteContainer(anyString());
    }

    @Test
    @DisplayName("cleanup gracefully handles null sandbox or null container details")
    void cleanupNullSafe() {
        isolateExecutionService.cleanup(null);
        isolateExecutionService.cleanup(new IsolateSandBoxDetails(1, null));

        verify(dockerExecutionService, never()).execContainer(anyString(), anyList());
    }

    @Test
    @DisplayName("cleanup throws IsolateCleanupException when isolate --cleanup returns non-zero")
    void cleanupFailureNonZeroExit() {
        when(dockerExecutionService.execContainer(eq("cnt-12345"), anyList()))
                .thenReturn(new DockerExecutionResult(1L, "", "isolate: cannot remove directory"));

        assertThatThrownBy(() -> isolateExecutionService.cleanup(testSandbox))
                .isInstanceOf(IsolateCleanupException.class)
                .hasMessageContaining("Failed to cleanup Isolate box 55: isolate: cannot remove directory");

        verify(dockerExecutionService, never()).stopContainer(anyString());
        verify(dockerExecutionService, never()).deleteContainer(anyString());
    }

    @Test
    @DisplayName("cleanup wraps Docker exception into IsolateCleanupException")
    void cleanupFailureDockerException() {
        when(dockerExecutionService.execContainer(eq("cnt-12345"), anyList()))
                .thenThrow(new DockerExecutionException("Docker communication failed"));

        assertThatThrownBy(() -> isolateExecutionService.cleanup(testSandbox))
                .isInstanceOf(IsolateCleanupException.class)
                .hasMessageContaining("Failed to cleanup Isolate sandbox: 55")
                .hasCauseInstanceOf(DockerExecutionException.class);
    }

    // =========================================================================
    // Execution With Constraints Tests
    // =========================================================================

    @Test
    @DisplayName("executeWithConstraints writes stdin, executes command, reads meta/out/err, and parses result")
    void executeWithConstraintsSuccess() {
        String stdin = "42 100\n";
        List<String> command = List.of("./solution");
        IsolateExecutionConstraints constraints = new IsolateExecutionConstraints(
                1.5,
                3.0,
                131072L,
                4,
                2048L);

        String boxDir = "/var/lib/isolate/55/box";
        String metaContent = "time:0.020\ntime-wall:0.025\nmax-rss:12400\nexitcode:0\n";
        String stdoutContent = "142\n";
        String stderrContent = "";

        when(dockerExecutionService.execContainer(eq("cnt-12345"), anyList()))
                .thenReturn(new DockerExecutionResult(0L, "", ""));
        when(dockerExecutionService.readFile("cnt-12345", boxDir + "/meta.txt"))
                .thenReturn(metaContent);
        when(dockerExecutionService.readFile("cnt-12345", boxDir + "/stdout.txt"))
                .thenReturn(stdoutContent);
        when(dockerExecutionService.readFile("cnt-12345", boxDir + "/stderr.txt"))
                .thenReturn(stderrContent);

        IsolateExecutionResult expectedResult = new IsolateExecutionResult(
                IsolateExecutionStatus.SUCCESS,
                stdoutContent,
                stderrContent,
                0.020,
                0.025,
                12400L,
                0L,
                0L,
                false,
                4L,
                2L);
        when(isolateMetadataParser.parseMetadata(metaContent, stdoutContent, stderrContent))
                .thenReturn(expectedResult);

        IsolateExecutionResult result = isolateExecutionService.executeWithConstraints(
                testSandbox,
                command,
                stdin,
                constraints);

        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(expectedResult);

        // Verify stdin file write
        verify(dockerExecutionService).writeFile("cnt-12345", boxDir + "/stdin.txt", stdin);

        // Verify isolate command generation
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<String>> cmdCaptor = ArgumentCaptor.forClass(List.class);
        verify(dockerExecutionService).execContainer(eq("cnt-12345"), cmdCaptor.capture());

        List<String> capturedCmd = cmdCaptor.getValue();
        assertThat(capturedCmd).contains(
                "isolate",
                "--box-id=55",
                "--time=1.5",
                "--wall-time=3.0",
                "--mem=131072",
                "--processes=4",
                "--fsize=2048",
                "--meta=" + boxDir + "/meta.txt",
                "--stdin=stdin.txt",
                "--stdout=stdout.txt",
                "--stderr=stderr.txt",
                "--dir=/etc:maybe",
                "--full-env",
                "--run",
                "--",
                "./solution");
    }

    @Test
    @DisplayName("executeWithConstraints handles null stdin by writing empty string")
    void executeWithConstraintsNullStdin() {
        List<String> command = List.of("echo", "hello");
        IsolateExecutionConstraints constraints = new IsolateExecutionConstraints();
        String boxDir = "/var/lib/isolate/55/box";

        when(dockerExecutionService.execContainer(eq("cnt-12345"), anyList()))
                .thenReturn(new DockerExecutionResult(0L, "", ""));
        when(dockerExecutionService.readFile("cnt-12345", boxDir + "/meta.txt")).thenReturn("exitcode:0");
        when(dockerExecutionService.readFile("cnt-12345", boxDir + "/stdout.txt")).thenReturn("hello\n");
        when(dockerExecutionService.readFile("cnt-12345", boxDir + "/stderr.txt")).thenReturn("");

        when(isolateMetadataParser.parseMetadata(anyString(), anyString(), anyString()))
                .thenReturn(new IsolateExecutionResult(
                        IsolateExecutionStatus.SUCCESS, "hello\n", "", null, null, null, 0L, null, false, null, null));

        IsolateExecutionResult result = isolateExecutionService.executeWithConstraints(
                testSandbox,
                command,
                null,
                constraints);

        assertThat(result).isNotNull();
        verify(dockerExecutionService).writeFile("cnt-12345", boxDir + "/stdin.txt", "");
    }

    @Test
    @DisplayName("executeWithConstraints wraps execution failures into IsolateExecutionException")
    void executeWithConstraintsFailure() {
        List<String> command = List.of("./broken");
        IsolateExecutionConstraints constraints = new IsolateExecutionConstraints();

        doThrow(new DockerExecutionException("Failed to write stdin"))
                .when(dockerExecutionService).writeFile(anyString(), anyString(), anyString());

        assertThatThrownBy(() -> isolateExecutionService.executeWithConstraints(
                testSandbox,
                command,
                "input",
                constraints))
                .isInstanceOf(IsolateExecutionException.class)
                .hasMessageContaining("Failed to execute command in Isolate sandbox: 55")
                .hasCauseInstanceOf(DockerExecutionException.class);
    }
}
