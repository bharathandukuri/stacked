package com.bharath.stacked.modules.execution;

import com.bharath.stacked.modules.execution.dto.CodeExecutionConstraints;
import com.bharath.stacked.modules.execution.dto.DatabaseContainerConstraints;
import com.bharath.stacked.modules.execution.dto.request.SimpleCodeExecutionRequest;
import com.bharath.stacked.modules.execution.dto.response.SimpleCodeExecutionResult;
import com.bharath.stacked.modules.execution.enums.CodeExecutionStatus;
import com.bharath.stacked.modules.language.Language;
import com.bharath.stacked.modules.language.factory.impl.LanguageFactoryImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Code Execution DTOs and Enums Unit Tests")
class CodeExecutionModelsTest {

    private final LanguageFactoryImpl languageFactory = new LanguageFactoryImpl();

    @Test
    @DisplayName("CodeExecutionStatus enum contains all expected statuses")
    void codeExecutionStatusEnum() {
        assertThat(CodeExecutionStatus.values()).containsExactlyInAnyOrder(
                CodeExecutionStatus.SUCCESS,
                CodeExecutionStatus.COMPILATION_ERROR,
                CodeExecutionStatus.TIME_LIMIT_EXCEEDED,
                CodeExecutionStatus.MEMORY_LIMIT_EXCEEDED,
                CodeExecutionStatus.RUNTIME_ERROR,
                CodeExecutionStatus.SYSTEM_ERROR);

        assertThat(CodeExecutionStatus.valueOf("COMPILATION_ERROR"))
                .isEqualTo(CodeExecutionStatus.COMPILATION_ERROR);
    }

    @Test
    @DisplayName("CodeExecutionConstraints record constructor and accessors")
    void codeExecutionConstraintsRecord() {
        CodeExecutionConstraints c1 = new CodeExecutionConstraints(1500L);
        CodeExecutionConstraints c2 = new CodeExecutionConstraints(1500L, 131072L);
        CodeExecutionConstraints c3 = new CodeExecutionConstraints(1500L, 131072L);

        assertThat(c1.timeLimitMs()).isEqualTo(1500L);
        assertThat(c1.memoryLimitKb()).isNull();

        assertThat(c2.timeLimitMs()).isEqualTo(1500L);
        assertThat(c2.memoryLimitKb()).isEqualTo(131072L);

        assertThat(c2).isEqualTo(c3);
        assertThat(c2.hashCode()).isEqualTo(c3.hashCode());
        assertThat(c1).isNotEqualTo(c2);
        assertThat(c2.toString()).contains("1500").contains("131072");
    }

    @Test
    @DisplayName("DatabaseContainerConstraints record, builder, defaults, and accessors")
    void databaseContainerConstraintsRecord() {
        DatabaseContainerConstraints defaults = DatabaseContainerConstraints.defaults();
        assertThat(defaults.cpuLimit()).isEqualTo(1L);
        assertThat(defaults.memoryLimitKb()).isEqualTo(262144L);
        assertThat(defaults.pidsLimit()).isEqualTo(100L);
        assertThat(defaults.networkDisabled()).isTrue();

        DatabaseContainerConstraints custom = DatabaseContainerConstraints.builder()
                .cpuLimit(2L)
                .memoryLimitKb(524288L)
                .pidsLimit(200L)
                .networkDisabled(false)
                .build();

        assertThat(custom.cpuLimit()).isEqualTo(2L);
        assertThat(custom.memoryLimitKb()).isEqualTo(524288L);
        assertThat(custom.pidsLimit()).isEqualTo(200L);
        assertThat(custom.networkDisabled()).isFalse();

        DatabaseContainerConstraints same = new DatabaseContainerConstraints(2L, 524288L, 200L, false);
        assertThat(custom).isEqualTo(same);
        assertThat(custom.hashCode()).isEqualTo(same.hashCode());
        assertThat(custom.toString()).contains("524288");
    }

    @Test
    @DisplayName("SimpleCodeExecutionRequest constructor, builder, and getters")
    void simpleCodeExecutionRequest() {
        Language java = languageFactory.create("java-21");
        CodeExecutionConstraints constraints = new CodeExecutionConstraints(2000L);

        SimpleCodeExecutionRequest request = SimpleCodeExecutionRequest.builder()
                .language(java)
                .code("System.out.println(\"test\");")
                .fileName("Main.java")
                .stdin("sample input")
                .constraints(constraints)
                .build();

        assertThat(request.getLanguage()).isEqualTo(java);
        assertThat(request.getCode()).isEqualTo("System.out.println(\"test\");");
        assertThat(request.getFileName()).isEqualTo("Main.java");
        assertThat(request.getStdin()).isEqualTo("sample input");
        assertThat(request.getConstraints()).isEqualTo(constraints);

        // Test setters and no-arg constructor
        SimpleCodeExecutionRequest emptyReq = new SimpleCodeExecutionRequest();
        emptyReq.setLanguage(java);
        emptyReq.setCode("print('hello')");
        emptyReq.setFileName("test.py");
        emptyReq.setStdin("in");
        emptyReq.setConstraints(constraints);

        assertThat(emptyReq.getLanguage()).isEqualTo(java);
        assertThat(emptyReq.getCode()).isEqualTo("print('hello')");
        assertThat(emptyReq.getFileName()).isEqualTo("test.py");
        assertThat(emptyReq.getStdin()).isEqualTo("in");
        assertThat(emptyReq.getConstraints()).isEqualTo(constraints);
    }

    @Test
    @DisplayName("SimpleCodeExecutionResult record, builder, equals, and toString")
    void simpleCodeExecutionResultRecord() {
        SimpleCodeExecutionResult result1 = SimpleCodeExecutionResult.builder()
                .stdout("Output")
                .stderr("")
                .exitCode(0L)
                .exitSignal(0L)
                .executionStatus(CodeExecutionStatus.SUCCESS)
                .logs(List.of("Execution succeeded"))
                .build();

        SimpleCodeExecutionResult result2 = SimpleCodeExecutionResult.builder()
                .stdout("Output")
                .stderr("")
                .exitCode(0L)
                .exitSignal(0L)
                .executionStatus(CodeExecutionStatus.SUCCESS)
                .logs(List.of("Execution succeeded"))
                .build();

        assertThat(result1.stdout()).isEqualTo("Output");
        assertThat(result1.stderr()).isEmpty();
        assertThat(result1.exitCode()).isEqualTo(0L);
        assertThat(result1.exitSignal()).isEqualTo(0L);
        assertThat(result1.executionStatus()).isEqualTo(CodeExecutionStatus.SUCCESS);
        assertThat(result1.logs()).containsExactly("Execution succeeded");

        assertThat(result1).isEqualTo(result2);
        assertThat(result1.hashCode()).isEqualTo(result2.hashCode());
        assertThat(result1.toString()).contains("Output").contains("SUCCESS");
    }
}
