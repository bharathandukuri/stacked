package com.bharath.stacked.modules.execution.service.impl;

import com.bharath.stacked.modules.execution.config.DockerConfig;
import com.bharath.stacked.modules.execution.config.DockerProperties;
import com.bharath.stacked.modules.execution.dto.request.SimpleCodeExecutionRequest;
import com.bharath.stacked.modules.execution.dto.response.SimpleCodeExecutionResult;
import com.bharath.stacked.modules.execution.enums.CodeExecutionStatus;
import com.bharath.stacked.modules.execution.dto.CodeExecutionConstraints;
import com.bharath.stacked.modules.execution.dto.DockerImageDetails;
import com.bharath.stacked.modules.execution.mapper.IsolateMetadataParser;
import com.bharath.stacked.modules.execution.service.CodeExecutionService;
import com.bharath.stacked.modules.execution.mapper.CodeExecutionStatusMapper;
import com.bharath.stacked.modules.execution.service.DockerExecutionService;
import com.bharath.stacked.modules.execution.service.IsolateExecutionService;
import com.bharath.stacked.modules.language.Language;
import com.bharath.stacked.modules.language.factory.impl.LanguageFactoryImpl;
import com.bharath.stacked.modules.language.registry.LanguageRegistry;
import com.bharath.stacked.modules.language.registry.impl.LanguageRegistryImpl;
import com.github.dockerjava.api.DockerClient;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CodeExecutionService Live Integration Tests")
class CodeExecutionServiceIntegrationTest {

    private static DockerClient dockerClient;
    private static DockerProperties dockerProperties;
    private static DockerExecutionService dockerExecutionService;
    private static IsolateExecutionService isolateExecutionService;
    private static CodeExecutionStatusMapper statusMapper;
    private static CodeExecutionService codeExecutionService;
    private static LanguageRegistry languageRegistry;
    private static boolean dockerAvailable;

    @BeforeAll
    static void initDocker() {
        dockerProperties = new DockerProperties();
        dockerProperties.setEnabled(true);
        dockerProperties.setHost("unix:///var/run/docker.sock");

        try {
            DockerConfig config = new DockerConfig();
            dockerClient = config.dockerClient(dockerProperties);
            dockerClient.pingCmd().exec();

            dockerExecutionService = new DockerExecutionServiceImpl(dockerClient, dockerProperties);
            IsolateMetadataParser metadataParser = new IsolateMetadataParser();
            isolateExecutionService = new IsolateExecutionServiceImpl(dockerExecutionService, metadataParser);
            statusMapper = new CodeExecutionStatusMapper();
            codeExecutionService = new CodeExecutionServiceImpl(dockerExecutionService, isolateExecutionService,
                    statusMapper);

            languageRegistry = new LanguageRegistryImpl(new LanguageFactoryImpl());
            ((LanguageRegistryImpl) languageRegistry).init();

            dockerAvailable = true;
        } catch (Exception e) {
            dockerAvailable = false;
        }
    }

    private void assumeLanguageImageAvailable(Language language) {
        Assumptions.assumeTrue(dockerAvailable, "Docker daemon is not available; skipping live integration tests.");
        DockerImageDetails img = language.dockerImageDetails();
        Assumptions.assumeTrue(dockerExecutionService.isImageExists(img),
                "Required Docker image [" + img.reference() + "] is not available in local daemon.");
    }

    @Test
    @DisplayName("Java 21: successfully compiles and executes with stdin, stdout, and cleans up container")
    void executeJava21Program() {
        Language java = languageRegistry.get("java-21");
        assumeLanguageImageAvailable(java);

        String code = """
                import java.util.Scanner;
                public class Solution {
                    public static void main(String[] args) {
                        Scanner sc = new Scanner(System.in);
                        int a = sc.nextInt();
                        int b = sc.nextInt();
                        System.out.println("RESULT=" + (a * b));
                    }
                }
                """;

        SimpleCodeExecutionRequest request = SimpleCodeExecutionRequest.builder()
                .language(java)
                .code(code)
                .fileName("Solution.java")
                .stdin("6 7\n")
                .constraints(new CodeExecutionConstraints(5000L, 524288L))
                .build();

        SimpleCodeExecutionResult result = codeExecutionService.run(request);

        assertThat(result).isNotNull();
        assertThat(result.executionStatus())
                .withFailMessage("STDERR: [%s], STDOUT: [%s], LOGS: [%s]", result.stderr(), result.stdout(),
                        result.logs())
                .isEqualTo(CodeExecutionStatus.SUCCESS);
        assertThat(result.exitCode()).isEqualTo(0L);
        assertThat(result.stdout()).contains("RESULT=42");
        assertThat(result.stderr()).isEmpty();
        assertThat(result.logs()).isNotEmpty();
    }

    @Test
    @DisplayName("Java 21: compilation failure returns COMPILATION_ERROR, stops execution, and captures error")
    void executeJava21CompilationError() {
        Language java = languageRegistry.get("java-21");
        assumeLanguageImageAvailable(java);

        String code = """
                public class Solution {
                    public static void main(String[] args) {
                        invalidSyntaxStatementDoesNotExist;
                    }
                }
                """;

        SimpleCodeExecutionRequest request = SimpleCodeExecutionRequest.builder()
                .language(java)
                .code(code)
                .fileName("Solution.java")
                .build();

        SimpleCodeExecutionResult result = codeExecutionService.run(request);

        assertThat(result).isNotNull();
        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.COMPILATION_ERROR);
        assertThat(result.exitCode()).isNotNull().isNotEqualTo(0L);
        assertThat(result.stderr()).contains("not a statement");
        assertThat(result.logs()).contains("Compilation failed with exit code: " + result.exitCode());
    }

    @Test
    @DisplayName("Python 3.12: successfully executes with stdin and stdout")
    void executePython312Program() {
        Language python = languageRegistry.get("python-3.12");
        assumeLanguageImageAvailable(python);

        String code = """
                import sys
                lines = sys.stdin.read().split()
                if lines:
                    a, b = map(int, lines[:2])
                    print(f"PRODUCT={a * b}")
                """;

        SimpleCodeExecutionRequest request = SimpleCodeExecutionRequest.builder()
                .language(python)
                .code(code)
                .fileName("solution.py")
                .stdin("9 8\n")
                .constraints(new CodeExecutionConstraints(3000L, 131072L))
                .build();

        SimpleCodeExecutionResult result = codeExecutionService.run(request);

        assertThat(result).isNotNull();
        assertThat(result.executionStatus())
                .withFailMessage("STDERR: [%s], STDOUT: [%s]", result.stderr(), result.stdout())
                .isEqualTo(CodeExecutionStatus.SUCCESS);
        assertThat(result.exitCode()).isEqualTo(0L);
        assertThat(result.stdout()).contains("PRODUCT=72");
    }

    @Test
    @DisplayName("Python 3.12: runtime exception returns RUNTIME_ERROR with traceback in stderr")
    void executePython312RuntimeError() {
        Language python = languageRegistry.get("python-3.12");
        assumeLanguageImageAvailable(python);

        String code = """
                x = 10 / 0
                """;

        SimpleCodeExecutionRequest request = SimpleCodeExecutionRequest.builder()
                .language(python)
                .code(code)
                .fileName("solution.py")
                .build();

        SimpleCodeExecutionResult result = codeExecutionService.run(request);

        assertThat(result).isNotNull();
        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.RUNTIME_ERROR);
        assertThat(result.exitCode()).isNotNull().isNotEqualTo(0L);
        assertThat(result.stderr()).contains("ZeroDivisionError: division by zero");
    }

    @Test
    @DisplayName("C++ 23: successfully compiles and executes standard library features")
    void executeCpp23Program() {
        Language cpp = languageRegistry.get("cpp-23");
        assumeLanguageImageAvailable(cpp);

        String code = """
                #include <iostream>
                #include <vector>
                #include <numeric>

                int main() {
                    int a, b;
                    if (std::cin >> a >> b) {
                        std::cout << "CPP_SUM=" << (a + b) << std::endl;
                    }
                    return 0;
                }
                """;

        SimpleCodeExecutionRequest request = SimpleCodeExecutionRequest.builder()
                .language(cpp)
                .code(code)
                .fileName("solution.cpp")
                .stdin("100 250\n")
                .constraints(new CodeExecutionConstraints(3000L, 131072L))
                .build();

        SimpleCodeExecutionResult result = codeExecutionService.run(request);

        assertThat(result).isNotNull();
        assertThat(result.executionStatus())
                .withFailMessage("STDERR: [%s], STDOUT: [%s]", result.stderr(), result.stdout())
                .isEqualTo(CodeExecutionStatus.SUCCESS);
        assertThat(result.exitCode()).isEqualTo(0L);
        assertThat(result.stdout()).contains("CPP_SUM=350");
    }

    @Test
    @DisplayName("C 17: successfully compiles and executes C standard library program")
    void executeC17Program() {
        Language c = languageRegistry.get("c-17");
        assumeLanguageImageAvailable(c);

        String code = """
                #include <stdio.h>

                int main() {
                    int a, b;
                    if (scanf("%d %d", &a, &b) == 2) {
                        printf("C_DIFF=%d\\n", a - b);
                    }
                    return 0;
                }
                """;

        SimpleCodeExecutionRequest request = SimpleCodeExecutionRequest.builder()
                .language(c)
                .code(code)
                .fileName("solution.c")
                .stdin("50 18\n")
                .constraints(new CodeExecutionConstraints(3000L, 131072L))
                .build();

        SimpleCodeExecutionResult result = codeExecutionService.run(request);

        assertThat(result).isNotNull();
        assertThat(result.executionStatus())
                .withFailMessage("STDERR: [%s], STDOUT: [%s]", result.stderr(), result.stdout())
                .isEqualTo(CodeExecutionStatus.SUCCESS);
        assertThat(result.exitCode()).isEqualTo(0L);
        assertThat(result.stdout()).contains("C_DIFF=32");
    }

    @Test
    @DisplayName("JavaScript (Node.js 20): successfully executes script with standard I/O")
    void executeJavaScriptNode20Program() {
        Language js = languageRegistry.get("javascript-node-20");
        assumeLanguageImageAvailable(js);

        String code = """
                const fs = require('fs');
                const input = fs.readFileSync(0, 'utf-8').trim().split(/\\s+/);
                if (input.length >= 2) {
                    const [a, b] = input.map(Number);
                    console.log(`JS_SUM=${a + b}`);
                }
                """;

        SimpleCodeExecutionRequest request = SimpleCodeExecutionRequest.builder()
                .language(js)
                .code(code)
                .fileName("solution.js")
                .stdin("40 60\n")
                .constraints(new CodeExecutionConstraints(3000L, 262144L))
                .build();

        SimpleCodeExecutionResult result = codeExecutionService.run(request);

        assertThat(result).isNotNull();
        assertThat(result.executionStatus())
                .withFailMessage("STDERR: [%s], STDOUT: [%s]", result.stderr(), result.stdout())
                .isEqualTo(CodeExecutionStatus.SUCCESS);
        assertThat(result.exitCode()).isEqualTo(0L);
        assertThat(result.stdout()).contains("JS_SUM=100");
    }

    @Test
    @DisplayName("PostgreSQL 16: successfully executes SQL query and cleans up container")
    void executePostgres16LiveSuccess() {
        Language pg = languageRegistry.get("postgresql-16");
        assumeLanguageImageAvailable(pg);

        String code = "SELECT 42 * 3 AS res;";
        SimpleCodeExecutionRequest request = SimpleCodeExecutionRequest.builder()
                .language(pg)
                .code(code)
                .fileName("solution.sql")
                .constraints(new CodeExecutionConstraints(10000L))
                .build();

        SimpleCodeExecutionResult result = codeExecutionService.run(request);

        assertThat(result).isNotNull();
        assertThat(result.executionStatus())
                .withFailMessage("STDERR: [%s], STDOUT: [%s]", result.stderr(), result.stdout())
                .isEqualTo(CodeExecutionStatus.SUCCESS);
        assertThat(result.exitCode()).isEqualTo(0L);
        assertThat(result.stdout()).contains("126");
    }

    @Test
    @DisplayName("PostgreSQL 16: SQL syntax/schema error returns RUNTIME_ERROR with database error message")
    void executePostgres16LiveSyntaxError() {
        Language pg = languageRegistry.get("postgresql-16");
        assumeLanguageImageAvailable(pg);

        String code = "SELECT * FROM non_existing_table_xyz;";
        SimpleCodeExecutionRequest request = SimpleCodeExecutionRequest.builder()
                .language(pg)
                .code(code)
                .fileName("solution.sql")
                .constraints(new CodeExecutionConstraints(10000L))
                .build();

        SimpleCodeExecutionResult result = codeExecutionService.run(request);

        assertThat(result).isNotNull();
        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.RUNTIME_ERROR);
        assertThat(result.stderr()).contains("relation \"non_existing_table_xyz\" does not exist");
    }

    @Test
    @DisplayName("PostgreSQL 16: long-running query times out and returns TIME_LIMIT_EXCEEDED (124)")
    void executePostgres16LiveTimeout() {
        Language pg = languageRegistry.get("postgresql-16");
        assumeLanguageImageAvailable(pg);

        String code = "SELECT pg_sleep(5);";
        SimpleCodeExecutionRequest request = SimpleCodeExecutionRequest.builder()
                .language(pg)
                .code(code)
                .fileName("solution.sql")
                .constraints(new CodeExecutionConstraints(1000L))
                .build();

        SimpleCodeExecutionResult result = codeExecutionService.run(request);

        assertThat(result).isNotNull();
        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.TIME_LIMIT_EXCEEDED);
        assertThat(result.exitCode()).isEqualTo(124L);
    }

    @Test
    @DisplayName("C++ 23: infinite loop triggers TIME_LIMIT_EXCEEDED in Isolate sandbox")
    void executeCpp23LiveInfiniteLoopTimeout() {
        Language cpp = languageRegistry.get("cpp-23");
        assumeLanguageImageAvailable(cpp);

        String code = """
                int main() {
                    volatile int i = 0;
                    while (true) {
                        i++;
                    }
                    return 0;
                }
                """;

        SimpleCodeExecutionRequest request = SimpleCodeExecutionRequest.builder()
                .language(cpp)
                .code(code)
                .fileName("solution.cpp")
                .constraints(new CodeExecutionConstraints(1000L, 131072L))
                .build();

        SimpleCodeExecutionResult result = codeExecutionService.run(request);

        assertThat(result).isNotNull();
        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.TIME_LIMIT_EXCEEDED);
    }

    @Test
    @DisplayName("Python 3.12: infinite loop triggers TIME_LIMIT_EXCEEDED in Isolate sandbox")
    void executePython312LiveInfiniteLoopTimeout() {
        Language python = languageRegistry.get("python-3.12");
        assumeLanguageImageAvailable(python);

        String code = "while True: pass";
        SimpleCodeExecutionRequest request = SimpleCodeExecutionRequest.builder()
                .language(python)
                .code(code)
                .fileName("solution.py")
                .constraints(new CodeExecutionConstraints(1000L, 131072L))
                .build();

        SimpleCodeExecutionResult result = codeExecutionService.run(request);

        assertThat(result).isNotNull();
        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.TIME_LIMIT_EXCEEDED);
    }

    @Test
    @DisplayName("C++ 23: excessive memory allocation triggers MEMORY_LIMIT_EXCEEDED or RUNTIME_ERROR")
    void executeCpp23LiveMemoryLimitExceeded() {
        Language cpp = languageRegistry.get("cpp-23");
        assumeLanguageImageAvailable(cpp);

        String code = """
                #include <vector>
                int main() {
                    std::vector<char> v(100 * 1024 * 1024, 1);
                    return 0;
                }
                """;

        SimpleCodeExecutionRequest request = SimpleCodeExecutionRequest.builder()
                .language(cpp)
                .code(code)
                .fileName("solution.cpp")
                .constraints(new CodeExecutionConstraints(3000L, 32768L))
                .build();

        SimpleCodeExecutionResult result = codeExecutionService.run(request);

        assertThat(result).isNotNull();
        assertThat(result.executionStatus()).isIn(
                CodeExecutionStatus.MEMORY_LIMIT_EXCEEDED,
                CodeExecutionStatus.RUNTIME_ERROR
        );
        assertThat(result.exitSignal() != null || (result.exitCode() != null && result.exitCode() != 0L)).isTrue();
    }

    @Test
    @DisplayName("C++ 23: segmentation fault returns RUNTIME_ERROR with exitSignal 11")
    void executeCpp23LiveSegmentationFault() {
        Language cpp = languageRegistry.get("cpp-23");
        assumeLanguageImageAvailable(cpp);

        String code = """
                int main() {
                    int* p = nullptr;
                    *p = 42;
                    return 0;
                }
                """;

        SimpleCodeExecutionRequest request = SimpleCodeExecutionRequest.builder()
                .language(cpp)
                .code(code)
                .fileName("solution.cpp")
                .constraints(new CodeExecutionConstraints(3000L, 131072L))
                .build();

        SimpleCodeExecutionResult result = codeExecutionService.run(request);

        assertThat(result).isNotNull();
        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.RUNTIME_ERROR);
        assertThat(result.exitSignal()).isEqualTo(11L);
    }

    @Test
    @DisplayName("Java 21: high-throughput large stdout stream executes and captures output")
    void executeJava21LiveLargeOutput() {
        Language java = languageRegistry.get("java-21");
        assumeLanguageImageAvailable(java);

        String code = """
                public class Solution {
                    public static void main(String[] args) {
                        for (int i = 0; i < 5000; i++) {
                            System.out.println("LINE_" + i);
                        }
                    }
                }
                """;

        SimpleCodeExecutionRequest request = SimpleCodeExecutionRequest.builder()
                .language(java)
                .code(code)
                .fileName("Solution.java")
                .constraints(new CodeExecutionConstraints(5000L, 524288L))
                .build();

        SimpleCodeExecutionResult result = codeExecutionService.run(request);

        assertThat(result).isNotNull();
        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.SUCCESS);
        assertThat(result.exitCode()).isEqualTo(0L);
        assertThat(result.stdout()).contains("LINE_0").contains("LINE_4999");
    }
}
