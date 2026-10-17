package com.bharath.stacked.modules.execution.service.impl;

import com.bharath.stacked.modules.execution.enums.IsolateExecutionStatus;
import com.bharath.stacked.modules.execution.dto.response.IsolateExecutionResult;
import com.bharath.stacked.modules.execution.mapper.IsolateMetadataParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("IsolateMetadataParserServiceImpl Unit Tests")
class IsolateMetadataParserTest {

    private IsolateMetadataParser parserService;

    @BeforeEach
    void setUp() {
        parserService = new IsolateMetadataParser();
    }

    @Test
    @DisplayName("parseMetadata successfully parses full metadata with valid metrics")
    void parseMetadataFull() {
        String metadata = """
                time:0.045
                time-wall:0.052
                max-rss:14560
                exitcode:0
                exitsig:0
                killed:false
                csw-voluntary:18
                csw-forced:4
                """;

        String stdout = "Program output\nLine 2";
        String stderr = "";

        IsolateExecutionResult result = parserService.parseMetadata(metadata, stdout, stderr);

        assertThat(result.status()).isEqualTo(IsolateExecutionStatus.SUCCESS);
        assertThat(result.stdout()).isEqualTo(stdout);
        assertThat(result.stderr()).isEqualTo(stderr);
        assertThat(result.cpuTimeSeconds()).isEqualTo(0.045);
        assertThat(result.wallTimeSeconds()).isEqualTo(0.052);
        assertThat(result.memoryKb()).isEqualTo(14560L);
        assertThat(result.exitCode()).isEqualTo(0L);
        assertThat(result.exitSignal()).isEqualTo(0L);
        assertThat(result.killed()).isFalse();
        assertThat(result.contextSwitchesVoluntary()).isEqualTo(18L);
        assertThat(result.contextSwitchesForced()).isEqualTo(4L);
    }

    @ParameterizedTest
    @CsvSource({
            "RE, RUNTIME_ERROR",
            "SG, RUNTIME_ERROR",
            "TO, TIME_LIMIT_EXCEEDED",
            "ML, MEMORY_LIMIT_EXCEEDED",
            "XX, SYSTEM_ERROR",
            "UNKNOWN, SYSTEM_ERROR"
    })
    @DisplayName("parseMetadata maps status codes correctly")
    void parseMetadataStatusCodes(String statusCode, String expectedStatusName) {
        String metadata = "status:" + statusCode + "\nexitcode:1";

        IsolateExecutionResult result = parserService.parseMetadata(metadata, "", "");

        assertThat(result.status()).isEqualTo(IsolateExecutionStatus.valueOf(expectedStatusName));
        assertThat(result.exitCode()).isEqualTo(1L);
    }

    @Test
    @DisplayName("parseMetadata maps memory limit message to MEMORY_LIMIT_EXCEEDED")
    void parseMetadataMemoryLimitFromMessage() {
        String metadata = """
                status:RE
                message:Memory limit exceeded
                max-rss:262144
                """;

        IsolateExecutionResult result = parserService.parseMetadata(metadata, "", "");

        assertThat(result.status()).isEqualTo(IsolateExecutionStatus.MEMORY_LIMIT_EXCEEDED);
        assertThat(result.memoryKb()).isEqualTo(262144L);
    }

    @Test
    @DisplayName("parseMetadata handles killed=true flag")
    void parseMetadataKilledTrue() {
        String metadata = """
                status:TO
                killed:true
                """;

        IsolateExecutionResult result = parserService.parseMetadata(metadata, "", "");

        assertThat(result.status()).isEqualTo(IsolateExecutionStatus.TIME_LIMIT_EXCEEDED);
        assertThat(result.killed()).isTrue();
    }

    @Test
    @DisplayName("parseMetadata handles missing or empty metadata gracefully")
    void parseMetadataEmptyOrBlank() {
        IsolateExecutionResult result = parserService.parseMetadata("", "output", "error");

        assertThat(result.status()).isEqualTo(IsolateExecutionStatus.SUCCESS);
        assertThat(result.stdout()).isEqualTo("output");
        assertThat(result.stderr()).isEqualTo("error");
        assertThat(result.cpuTimeSeconds()).isNull();
        assertThat(result.wallTimeSeconds()).isNull();
        assertThat(result.memoryKb()).isNull();
        assertThat(result.exitCode()).isNull();
        assertThat(result.exitSignal()).isNull();
        assertThat(result.killed()).isNull();
        assertThat(result.contextSwitchesVoluntary()).isNull();
        assertThat(result.contextSwitchesForced()).isNull();
    }

    @Test
    @DisplayName("parseMetadata ignores malformed lines, lines without separator, or empty lines")
    void parseMetadataMalformedLines() {
        String metadata = """
                
                malformed_line_without_colon
                :missing_key_colon_at_start
                valid_key:123
                   \s
                another invalid line
                status:RE
                """;

        IsolateExecutionResult result = parserService.parseMetadata(metadata, "out", "err");

        assertThat(result.status()).isEqualTo(IsolateExecutionStatus.RUNTIME_ERROR);
    }

    @ParameterizedTest
    @ValueSource(strings = {"status:   ", "status:\n", "time:  \nstatus:"})
    @DisplayName("parseMetadata defaults to SUCCESS when status key has blank value and zero/no exitcode")
    void parseMetadataBlankStatusValue(String metadata) {
        IsolateExecutionResult result = parserService.parseMetadata(metadata, "", "");

        assertThat(result.status()).isEqualTo(IsolateExecutionStatus.SUCCESS);
    }

    @Test
    @DisplayName("parseMetadata maps to RUNTIME_ERROR when status is blank but exitcode is non-zero")
    void parseMetadataBlankStatusWithNonZeroExitCode() {
        String metadata = """
                exitcode:1
                time:0.012
                """;

        IsolateExecutionResult result = parserService.parseMetadata(metadata, "", "Process exited with code 1");

        assertThat(result.status()).isEqualTo(IsolateExecutionStatus.RUNTIME_ERROR);
        assertThat(result.exitCode()).isEqualTo(1L);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "terminate called after throwing an instance of 'std::bad_alloc'\n  what():  std::bad_alloc",
            "Traceback (most recent call last):\n  File 'sol.py', line 2\nMemoryError",
            "Exception in thread 'main' java.lang.OutOfMemoryError: Java heap space",
            "<--- JS stacktrace --->\nFATAL ERROR: Reached heap limit Allocation failed - JavaScript heap out of memory",
            "Out of memory: malloc failed"
    })
    @DisplayName("parseMetadata maps runtime errors with OOM patterns in stderr to MEMORY_LIMIT_EXCEEDED")
    void parseMetadataMemoryLimitFromStderr(String oomStderr) {
        String metadata = """
                status:RE
                exitcode:1
                time:0.045
                """;

        IsolateExecutionResult result = parserService.parseMetadata(metadata, "", oomStderr);

        assertThat(result.status()).isEqualTo(IsolateExecutionStatus.MEMORY_LIMIT_EXCEEDED);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Exception in thread 'main' java.lang.StackOverflowError",
            "RecursionError: maximum recursion depth exceeded while calling a Python object",
            "RangeError: Maximum call stack size exceeded"
    })
    @DisplayName("parseMetadata keeps stack exhaustion as RUNTIME_ERROR instead of misclassifying as MLE")
    void parseMetadataStackOverflowIsRuntimeError(String stackStderr) {
        String metadata = """
                status:RE
                exitcode:1
                time:0.045
                """;

        IsolateExecutionResult result = parserService.parseMetadata(metadata, "", stackStderr);

        assertThat(result.status()).isEqualTo(IsolateExecutionStatus.RUNTIME_ERROR);
    }
}
