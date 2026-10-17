package com.bharath.stacked.modules.execution.integration;

import com.bharath.stacked.modules.execution.dto.CodeExecutionConstraints;
import com.bharath.stacked.modules.execution.dto.request.BatchCodeExecutionRequest;
import com.bharath.stacked.modules.execution.dto.request.BatchExecutionItem;
import com.bharath.stacked.modules.execution.dto.request.SimpleCodeExecutionRequest;
import com.bharath.stacked.modules.execution.dto.response.BatchCodeExecutionResult;
import com.bharath.stacked.modules.execution.dto.response.SimpleCodeExecutionResult;
import com.bharath.stacked.modules.execution.enums.CodeExecutionStatus;
import com.bharath.stacked.modules.language.Language;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@DisplayName("Java 21 Comprehensive Execution Limit & Edge Case Tests")
public class JavaExecutionLimitIT extends BaseExecutionIT {

    @Test
    @DisplayName("Normal Execution: Reading stdin and producing correct stdout")
    void testJavaNormalExecution() {
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

        SimpleCodeExecutionResult result = codeExecutionService.run(SimpleCodeExecutionRequest.builder()
                .language(java)
                .code(code)
                .fileName("Solution.java")
                .stdin("15 27\n")
                .constraints(new CodeExecutionConstraints(5000L, 262144L))
                .build());

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.SUCCESS);
        assertThat(result.stdout().trim()).isEqualTo("SUM=42");
        assertThat(result.exitCode()).isEqualTo(0L);
    }

    @Test
    @DisplayName("Empty Input: Handles empty stdin without crashing")
    void testJavaEmptyInput() {
        Language java = languageRegistry.get("java-21");
        assumeLanguageImageAvailable(java);

        String code = """
                import java.util.Scanner;
                public class Solution {
                    public static void main(String[] args) {
                        Scanner sc = new Scanner(System.in);
                        if (!sc.hasNext()) {
                            System.out.println("NO_INPUT");
                        }
                    }
                }
                """;

        SimpleCodeExecutionResult result = codeExecutionService.run(SimpleCodeExecutionRequest.builder()
                .language(java)
                .code(code)
                .fileName("Solution.java")
                .stdin("")
                .constraints(new CodeExecutionConstraints(3000L, 262144L))
                .build());

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.SUCCESS);
        assertThat(result.stdout().trim()).isEqualTo("NO_INPUT");
    }

    @Test
    @DisplayName("Large Input: Streams 20,000 integers via fast I/O")
    void testJavaLargeInput() {
        Language java = languageRegistry.get("java-21");
        assumeLanguageImageAvailable(java);

        String code = """
                import java.io.*;
                import java.util.StringTokenizer;

                public class Solution {
                    public static void main(String[] args) throws IOException {
                        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
                        String line = br.readLine();
                        if (line == null) return;
                        StringTokenizer st = new StringTokenizer(line);
                        long sum = 0;
                        int count = 0;
                        while (st.hasMoreTokens()) {
                            sum += Integer.parseInt(st.nextToken());
                            count++;
                        }
                        System.out.println("COUNT=" + count + " SUM=" + sum);
                    }
                }
                """;

        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= 20000; i++) {
            sb.append("1 ");
        }
        sb.append("\n");

        SimpleCodeExecutionResult result = codeExecutionService.run(SimpleCodeExecutionRequest.builder()
                .language(java)
                .code(code)
                .fileName("Solution.java")
                .stdin(sb.toString())
                .constraints(new CodeExecutionConstraints(4000L, 262144L))
                .build());

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.SUCCESS);
        assertThat(result.stdout().trim()).isEqualTo("COUNT=20000 SUM=20000");
    }

    @Test
    @DisplayName("Time Limit Exceeded: Infinite loop is terminated cleanly")
    void testJavaTimeLimitExceeded() {
        Language java = languageRegistry.get("java-21");
        assumeLanguageImageAvailable(java);

        String code = """
                public class Solution {
                    public static void main(String[] args) {
                        while (true) {}
                    }
                }
                """;

        SimpleCodeExecutionResult result = codeExecutionService.run(SimpleCodeExecutionRequest.builder()
                .language(java)
                .code(code)
                .fileName("Solution.java")
                .constraints(new CodeExecutionConstraints(1000L))
                .build());

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.TIME_LIMIT_EXCEEDED);
    }

    @Test
    @DisplayName("Memory Limit Exceeded: Heap space exhaustion mapped to MEMORY_LIMIT_EXCEEDED")
    void testJavaMemoryLimitExceeded() {
        Language java = languageRegistry.get("java-21");
        assumeLanguageImageAvailable(java);

        String code = """
                import java.util.ArrayList;
                import java.util.List;
                public class Solution {
                    public static void main(String[] args) {
                        List<byte[]> list = new ArrayList<>();
                        for (int i = 0; i < 50; i++) {
                            list.add(new byte[1024 * 1024]); // 1MB chunks
                        }
                    }
                }
                """;

        SimpleCodeExecutionResult result = codeExecutionService.run(SimpleCodeExecutionRequest.builder()
                .language(java)
                .code(code)
                .fileName("Solution.java")
                .constraints(new CodeExecutionConstraints(4000L, 32768L)) // 32MB limit (-Xmx24m)
                .build());

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.MEMORY_LIMIT_EXCEEDED);
    }

    @Test
    @DisplayName("Stack Exhaustion: StackOverflowError mapped to RUNTIME_ERROR (not MLE)")
    void testJavaStackExhaustion() {
        Language java = languageRegistry.get("java-21");
        assumeLanguageImageAvailable(java);

        String code = """
                public class Solution {
                    public static void recurse() {
                        recurse();
                    }
                    public static void main(String[] args) {
                        recurse();
                    }
                }
                """;

        SimpleCodeExecutionResult result = codeExecutionService.run(SimpleCodeExecutionRequest.builder()
                .language(java)
                .code(code)
                .fileName("Solution.java")
                .constraints(new CodeExecutionConstraints(3000L, 131072L))
                .build());

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.RUNTIME_ERROR);
        assertThat(result.stderr()).contains("StackOverflowError");
    }

    @Test
    @DisplayName("Runtime Error: ArithmeticException (/ by zero) returns RUNTIME_ERROR")
    void testJavaArithmeticException() {
        Language java = languageRegistry.get("java-21");
        assumeLanguageImageAvailable(java);

        String code = """
                public class Solution {
                    public static void main(String[] args) {
                        int x = 10 / 0;
                    }
                }
                """;

        SimpleCodeExecutionResult result = codeExecutionService.run(SimpleCodeExecutionRequest.builder()
                .language(java)
                .code(code)
                .fileName("Solution.java")
                .build());

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.RUNTIME_ERROR);
        assertThat(result.stderr()).contains("ArithmeticException");
    }

    @Test
    @DisplayName("Compilation Error: Syntax error returns COMPILATION_ERROR with diagnostic line")
    void testJavaCompilationError() {
        Language java = languageRegistry.get("java-21");
        assumeLanguageImageAvailable(java);

        SimpleCodeExecutionResult result = codeExecutionService.run(SimpleCodeExecutionRequest.builder()
                .language(java)
                .code("public class Solution { invalid syntax here }")
                .fileName("Solution.java")
                .build());

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.COMPILATION_ERROR);
        assertThat(result.stderr()).contains("error:");
    }

    @Test
    @DisplayName("Multi-File Java: Compiles and runs Solution.java using Helper.java")
    void testJavaMultiFile() {
        Language java = languageRegistry.get("java-21");
        assumeLanguageImageAvailable(java);

        String helper = """
                public class Helper {
                    public static String getGreeting(String name) {
                        return "Hello, " + name + "!";
                    }
                }
                """;
        String solution = """
                public class Solution {
                    public static void main(String[] args) {
                        System.out.println(Helper.getGreeting("Stacked"));
                    }
                }
                """;

        BatchCodeExecutionRequest request = BatchCodeExecutionRequest.builder()
                .language(java)
                .files(Map.of("Solution.java", solution, "Helper.java", helper))
                .executions(List.of(BatchExecutionItem.builder().stdin("").build()))
                .build();

        BatchCodeExecutionResult result = codeExecutionService.run(request);

        assertThat(result.successfulExecutions()).isEqualTo(1);
        assertThat(result.results().get(0).stdout().trim()).isEqualTo("Hello, Stacked!");
    }

    @Test
    @DisplayName("Batch Execution: Sequential runs reuse single compilation without re-compiling")
    void testJavaBatchSequential() {
        Language java = languageRegistry.get("java-21");
        assumeLanguageImageAvailable(java);

        String code = """
                import java.util.Scanner;
                public class Solution {
                    public static void main(String[] args) {
                        Scanner sc = new Scanner(System.in);
                        if (sc.hasNextInt()) {
                            System.out.println(sc.nextInt() * 10);
                        }
                    }
                }
                """;

        BatchCodeExecutionRequest request = BatchCodeExecutionRequest.builder()
                .language(java)
                .files(Map.of("Solution.java", code))
                .executions(List.of(
                        BatchExecutionItem.builder().stdin("3\n").build(),
                        BatchExecutionItem.builder().stdin("7\n").build()
                ))
                .build();

        BatchCodeExecutionResult result = codeExecutionService.run(request);

        assertThat(result.totalExecutions()).isEqualTo(2);
        assertThat(result.successfulExecutions()).isEqualTo(2);
        assertThat(result.results().get(0).stdout().trim()).isEqualTo("30");
        assertThat(result.results().get(1).stdout().trim()).isEqualTo("70");
    }
}

