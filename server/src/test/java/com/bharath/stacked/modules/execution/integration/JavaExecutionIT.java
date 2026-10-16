package com.bharath.stacked.modules.execution.integration;

import com.bharath.stacked.modules.execution.dto.CodeExecutionConstraints;
import com.bharath.stacked.modules.execution.dto.request.BatchCodeExecutionRequest;
import com.bharath.stacked.modules.execution.dto.request.BatchExecutionItem;
import com.bharath.stacked.modules.execution.dto.request.SimpleCodeExecutionRequest;
import com.bharath.stacked.modules.execution.dto.response.BatchCodeExecutionResult;
import com.bharath.stacked.modules.execution.dto.response.SimpleCodeExecutionResult;
import com.bharath.stacked.modules.execution.enums.CodeExecutionStatus;
import com.bharath.stacked.modules.language.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Java 21 Live Integration Tests")
class JavaExecutionIT extends BaseExecutionIT {

    @Test
    @DisplayName("Executes Java 21 single program with stdin and verifies JVM large heap pre-allocation does not crash")
    void executeJava21Success() {
        Language java = languageRegistry.get("java-21");
        assumeLanguageImageAvailable(java);

        String code = """
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

        SimpleCodeExecutionRequest request = SimpleCodeExecutionRequest.builder()
                .language(java)
                .code(code)
                .fileName("Solution.java")
                .stdin("15 27\n")
                .constraints(new CodeExecutionConstraints(5000L, 262144L))
                .build();

        SimpleCodeExecutionResult result = codeExecutionService.run(request);

        assertThat(result).isNotNull();
        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.SUCCESS);
        assertThat(result.stdout().trim()).isEqualTo("SUM=42");
        assertThat(result.exitCode()).isEqualTo(0L);
    }

    @Test
    @DisplayName("Java 21 batch execution runs multiple inputs sequentially within single container and compiles once")
    void executeJava21BatchSuccess() {
        Language java = languageRegistry.get("java-21");
        assumeLanguageImageAvailable(java);

        String code = """
                import java.util.Scanner;
                public class Solution {
                    public static void main(String[] args) {
                        Scanner sc = new Scanner(System.in);
                        if (sc.hasNextInt()) {
                            int n = sc.nextInt();
                            System.out.println(n * 10);
                        }
                    }
                }
                """;

        BatchCodeExecutionRequest request = BatchCodeExecutionRequest.builder()
                .language(java)
                .files(Map.of("Solution.java", code))
                .constraints(new CodeExecutionConstraints(5000L))
                .executions(List.of(
                        BatchExecutionItem.builder().stdin("1\n").build(),
                        BatchExecutionItem.builder().stdin("5\n").build(),
                        BatchExecutionItem.builder().stdin("9\n").build()
                ))
                .build();

        BatchCodeExecutionResult result = codeExecutionService.run(request);

        assertThat(result).isNotNull();
        assertThat(result.totalExecutions()).isEqualTo(3);
        assertThat(result.successfulExecutions()).isEqualTo(3);
        assertThat(result.failedExecutions()).isEqualTo(0);
        assertThat(result.results()).hasSize(3);

        assertThat(result.results().get(0).stdout().trim()).isEqualTo("10");
        assertThat(result.results().get(1).stdout().trim()).isEqualTo("50");
        assertThat(result.results().get(2).stdout().trim()).isEqualTo("90");
    }

    @Test
    @DisplayName("Java 21 compilation error marks all batch items as COMPILATION_ERROR")
    void executeJava21CompilationError() {
        Language java = languageRegistry.get("java-21");
        assumeLanguageImageAvailable(java);

        String invalidCode = "public class Solution { invalid syntax }";

        BatchCodeExecutionRequest request = BatchCodeExecutionRequest.builder()
                .language(java)
                .files(Map.of("Solution.java", invalidCode))
                .executions(List.of(
                        BatchExecutionItem.builder().stdin("1\n").build(),
                        BatchExecutionItem.builder().stdin("2\n").build()
                ))
                .build();

        BatchCodeExecutionResult result = codeExecutionService.run(request);

        assertThat(result.totalExecutions()).isEqualTo(2);
        assertThat(result.successfulExecutions()).isEqualTo(0);
        assertThat(result.failedExecutions()).isEqualTo(2);
        assertThat(result.results().get(0).executionStatus()).isEqualTo(CodeExecutionStatus.COMPILATION_ERROR);
        assertThat(result.results().get(1).executionStatus()).isEqualTo(CodeExecutionStatus.COMPILATION_ERROR);
        assertThat(result.results().get(0).stderr()).contains("error");
    }

    @Test
    @DisplayName("Java 21 enforces execution timeout for infinite loops")
    void executeJava21TimeLimitExceeded() {
        Language java = languageRegistry.get("java-21");
        assumeLanguageImageAvailable(java);

        String infiniteLoop = """
                public class Solution {
                    public static void main(String[] args) {
                        while (true) {}
                    }
                }
                """;

        SimpleCodeExecutionRequest request = SimpleCodeExecutionRequest.builder()
                .language(java)
                .code(infiniteLoop)
                .fileName("Solution.java")
                .constraints(new CodeExecutionConstraints(1000L))
                .build();

        SimpleCodeExecutionResult result = codeExecutionService.run(request);

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.TIME_LIMIT_EXCEEDED);
    }
}

