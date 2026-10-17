package com.bharath.stacked.modules.execution.integration;

import com.bharath.stacked.modules.execution.dto.CodeExecutionConstraints;
import com.bharath.stacked.modules.execution.dto.request.SimpleCodeExecutionRequest;
import com.bharath.stacked.modules.execution.dto.response.SimpleCodeExecutionResult;
import com.bharath.stacked.modules.execution.enums.CodeExecutionStatus;
import com.bharath.stacked.modules.language.Language;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@DisplayName("Execution Language Default Limits (LeetCode-style) Integration Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ExecutionDefaultLimitsIntegrationTest extends BaseExecutionIT {

    @Test
    @Order(1)
    @DisplayName("C++ 23 Default Limits (1000ms, 128MB): Intended algorithm passes when constraints are null")
    void testCppDefaultLimitsPass() {
        Language cpp = languageRegistry.get("cpp-23");
        assumeLanguageImageAvailable(cpp);

        String code = """
                #include <iostream>
                #include <vector>
                int main() {
                    std::vector<int> v(1000000, 42); // ~4MB allocation
                    long long sum = 0;
                    for (int x : v) sum += x;
                    std::cout << "SUM=" << sum << "\\n";
                    return 0;
                }
                """;

        // constraints explicitly omitted (null) -> engine applies language defaults
        SimpleCodeExecutionResult result = codeExecutionService.run(SimpleCodeExecutionRequest.builder()
                .language(cpp)
                .code(code)
                .fileName("solution.cpp")
                .constraints(null)
                .build());

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.SUCCESS);
        assertThat(result.stdout()).contains("SUM=42000000");
    }

    @Test
    @Order(2)
    @DisplayName("C++ 23 Default Limits (1000ms): Inefficient algorithm exceeds 1000ms default into TLE")
    void testCppDefaultLimitsTle() {
        Language cpp = languageRegistry.get("cpp-23");
        assumeLanguageImageAvailable(cpp);

        String code = """
                #include <iostream>
                int main() {
                    while (true) {}
                    return 0;
                }
                """;

        SimpleCodeExecutionResult result = codeExecutionService.run(SimpleCodeExecutionRequest.builder()
                .language(cpp)
                .code(code)
                .fileName("solution.cpp")
                .constraints(null) // Uses default 1000ms
                .build());

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.TIME_LIMIT_EXCEEDED);
    }

    @Test
    @Order(3)
    @DisplayName("C++ 23 Default Limits (128MB): Allocation exceeding 128MB default triggers MLE")
    void testCppDefaultLimitsMle() {
        Language cpp = languageRegistry.get("cpp-23");
        assumeLanguageImageAvailable(cpp);

        // Allocates ~200MB, exceeding default 128MB (131072 KB)
        String code = """
                #include <iostream>
                #include <vector>
                int main() {
                    std::vector<int> v(50 * 1024 * 1024); // 200MB
                    std::cout << v.size() << "\\n";
                    return 0;
                }
                """;

        SimpleCodeExecutionResult result = codeExecutionService.run(SimpleCodeExecutionRequest.builder()
                .language(cpp)
                .code(code)
                .fileName("solution.cpp")
                .constraints(null)
                .build());

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.MEMORY_LIMIT_EXCEEDED);
    }

    @Test
    @Order(4)
    @DisplayName("Java 21 Default Limits (2000ms, 256MB): Normal solution passes with default heap and no cold-start TLE")
    void testJavaDefaultLimitsPass() {
        Language java = languageRegistry.get("java-21");
        assumeLanguageImageAvailable(java);

        String code = """
                import java.util.ArrayList;
                import java.util.List;

                public class Solution {
                    public static void main(String[] args) {
                        List<Integer> list = new ArrayList<>();
                        for (int i = 0; i < 500_000; i++) list.add(i);
                        System.out.println("SIZE=" + list.size());
                    }
                }
                """;

        SimpleCodeExecutionResult result = codeExecutionService.run(SimpleCodeExecutionRequest.builder()
                .language(java)
                .code(code)
                .fileName("Solution.java")
                .constraints(null) // Uses default 2000ms, 256MB, -Xmx192m
                .build());

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.SUCCESS);
        assertThat(result.stdout().trim()).isEqualTo("SIZE=500000");
    }

    @Test
    @Order(5)
    @DisplayName("Java 21 Default Limits (256MB): Allocations exceeding 192MB heap trigger MLE")
    void testJavaDefaultLimitsMle() {
        Language java = languageRegistry.get("java-21");
        assumeLanguageImageAvailable(java);

        // Allocating 300MB exceeds default 192MB heap (-Xmx192m) -> OutOfMemoryError -> MLE
        String code = """
                import java.util.ArrayList;
                import java.util.List;

                public class Solution {
                    public static void main(String[] args) {
                        List<byte[]> list = new ArrayList<>();
                        for (int i = 0; i < 300; i++) {
                            list.add(new byte[1024 * 1024]); // 300MB
                        }
                        System.out.println("ALLOCATED=" + list.size());
                    }
                }
                """;

        SimpleCodeExecutionResult result = codeExecutionService.run(SimpleCodeExecutionRequest.builder()
                .language(java)
                .code(code)
                .fileName("Solution.java")
                .constraints(null)
                .build());

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.MEMORY_LIMIT_EXCEEDED);
    }

    @Test
    @Order(6)
    @DisplayName("Python 3.12 Default Limits (3000ms, 256MB): Intended algorithm passes under 3000ms without false TLE")
    void testPythonDefaultLimitsPass() {
        Language python = languageRegistry.get("python-3.12");
        assumeLanguageImageAvailable(python);

        String code = """
                total = sum(i for i in range(2_000_000))
                print(f"SUM={total}")
                """;

        SimpleCodeExecutionResult result = codeExecutionService.run(SimpleCodeExecutionRequest.builder()
                .language(python)
                .code(code)
                .fileName("solution.py")
                .constraints(null) // Uses default 3000ms
                .build());

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.SUCCESS);
        assertThat(result.stdout()).contains("SUM=1999999000000");
    }

    @Test
    @Order(7)
    @DisplayName("Python 3.12 Default Limits (3000ms): Inefficient algorithm exceeds 3000ms into TLE")
    void testPythonDefaultLimitsTle() {
        Language python = languageRegistry.get("python-3.12");
        assumeLanguageImageAvailable(python);

        // 100M iterations in Python takes ~5-6s -> TLE under 3000ms
        String code = """
                count = 0
                for i in range(100_000_000):
                    count += 1
                print(count)
                """;

        SimpleCodeExecutionResult result = codeExecutionService.run(SimpleCodeExecutionRequest.builder()
                .language(python)
                .code(code)
                .fileName("solution.py")
                .constraints(null)
                .build());

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.TIME_LIMIT_EXCEEDED);
    }

    @Test
    @Order(8)
    @DisplayName("JavaScript (Node.js 20) Default Limits (2000ms, 256MB): Standard algorithm passes")
    void testNodeJsDefaultLimitsPass() {
        Language node = languageRegistry.get("javascript-node-20");
        assumeLanguageImageAvailable(node);

        String code = """
                const arr = new Array(500000).fill(10);
                const sum = arr.reduce((acc, v) => acc + v, 0);
                console.log("SUM=" + sum);
                """;

        SimpleCodeExecutionResult result = codeExecutionService.run(SimpleCodeExecutionRequest.builder()
                .language(node)
                .code(code)
                .fileName("solution.js")
                .constraints(null) // Uses default 2000ms, 256MB
                .build());

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.SUCCESS);
        assertThat(result.stdout().trim()).isEqualTo("SUM=5000000");
    }

    @Test
    @Order(9)
    @DisplayName("SQLite 3 Default Limits (3000ms query timeout, 128MB container): Query passes")
    void testSqliteDefaultLimitsPass() {
        Language sqlite = languageRegistry.get("sqlite-3");
        assumeLanguageImageAvailable(sqlite);

        String sql = """
                CREATE TABLE numbers (val INT);
                INSERT INTO numbers VALUES (10), (20), (30);
                SELECT SUM(val) FROM numbers;
                """;

        SimpleCodeExecutionResult result = codeExecutionService.run(SimpleCodeExecutionRequest.builder()
                .language(sqlite)
                .code(sql)
                .fileName("query.sql")
                .constraints(null) // Uses default 3000ms query timeout
                .build());

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.SUCCESS);
        assertThat(result.stdout().trim()).contains("60");
    }

    @Test
    @Order(10)
    @DisplayName("Partial Problem Limit: Problem setter specifies only time limit, memory defaults cleanly")
    void testPartialProblemLimitTimeOnly() {
        Language cpp = languageRegistry.get("cpp-23");
        assumeLanguageImageAvailable(cpp);

        // Problem setter specifies 500ms time limit, memory unspecified
        // 500ms limit is respected -> while(true) quickly triggers TLE at 500ms
        SimpleCodeExecutionResult result = codeExecutionService.run(SimpleCodeExecutionRequest.builder()
                .language(cpp)
                .code("int main() { while(true) {} return 0; }")
                .fileName("solution.cpp")
                .constraints(new CodeExecutionConstraints(500L, null))
                .build());

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.TIME_LIMIT_EXCEEDED);
        if (result.executionTimeMs() != null) {
            assertThat(result.executionTimeMs()).isLessThanOrEqualTo(1500L);
        }
    }

    @Test
    @Order(11)
    @DisplayName("Partial Problem Limit: Problem setter specifies only memory limit, time defaults cleanly")
    void testPartialProblemLimitMemoryOnly() {
        Language cpp = languageRegistry.get("cpp-23");
        assumeLanguageImageAvailable(cpp);

        // Problem setter specifies 32MB memory limit, time unspecified (0)
        // 32MB limit is respected -> 50MB vector triggers MLE
        SimpleCodeExecutionResult result = codeExecutionService.run(SimpleCodeExecutionRequest.builder()
                .language(cpp)
                .code("""
                        #include <vector>
                        int main() {
                            std::vector<int> v(12 * 1024 * 1024); // ~48MB
                            return 0;
                        }
                        """)
                .fileName("solution.cpp")
                .constraints(new CodeExecutionConstraints(0L, 32768L))
                .build());

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.MEMORY_LIMIT_EXCEEDED);
    }
}

