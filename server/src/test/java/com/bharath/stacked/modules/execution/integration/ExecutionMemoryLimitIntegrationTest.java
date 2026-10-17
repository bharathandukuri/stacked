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
@DisplayName("Execution Memory Limit & MLE Pattern Integration Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ExecutionMemoryLimitIntegrationTest extends BaseExecutionIT {

    // =========================================================================
    // 1. O(1), O(log n), O(n) Memory Patterns - Passing
    // =========================================================================

    @Test
    @Order(1)
    @DisplayName("O(1) Constant Memory - Streaming Accumulation (C++ 23)")
    void testConstantMemoryPass() {
        Language cpp = languageRegistry.get("cpp-23");
        assumeLanguageImageAvailable(cpp);

        String code = """
                #include <iostream>
                int main() {
                    long long sum = 0;
                    for (int i = 0; i < 1000000; ++i) {
                        sum += (i % 7);
                    }
                    std::cout << "SUM=" << sum << "\\n";
                    return 0;
                }
                """;

        SimpleCodeExecutionResult result = codeExecutionService.run(SimpleCodeExecutionRequest.builder()
                .language(cpp)
                .code(code)
                .fileName("solution.cpp")
                .constraints(new CodeExecutionConstraints(2000L, 65536L))
                .build());

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.SUCCESS);
        assertThat(result.stdout()).contains("SUM=");
        if (result.memoryUsageKb() != null) {
            assertThat(result.memoryUsageKb()).isLessThan(10240L); // < 10 MB
        }
    }

    @Test
    @Order(2)
    @DisplayName("O(n) Memory - Controlled Allocation Within Budget (C++ 23)")
    void testLinearMemoryPass() {
        Language cpp = languageRegistry.get("cpp-23");
        assumeLanguageImageAvailable(cpp);

        // Allocating 8 MB vector within 64 MB limit -> PASS
        String code = """
                #include <iostream>
                #include <vector>
                int main() {
                    int elements = 2 * 1024 * 1024; // 8 MB of ints
                    std::vector<int> v(elements, 42);
                    std::cout << "SIZE=" << v.size() << " VAL=" << v[0] << "\\n";
                    return 0;
                }
                """;

        SimpleCodeExecutionResult result = codeExecutionService.run(SimpleCodeExecutionRequest.builder()
                .language(cpp)
                .code(code)
                .fileName("solution.cpp")
                .constraints(new CodeExecutionConstraints(3000L, 65536L))
                .build());

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.SUCCESS);
        assertThat(result.stdout().trim()).isEqualTo("SIZE=2097152 VAL=42");
        if (result.memoryUsageKb() != null) {
            assertThat(result.memoryUsageKb()).isGreaterThan(8000L); // >= 8 MB verified
            assertThat(result.memoryUsageKb()).isLessThan(65536L);
        }
    }

    @Test
    @Order(3)
    @DisplayName("O(n) Python Memory - Moderate List Within Budget (Python 3.12)")
    void testPythonModerateMemoryPass() {
        Language python = languageRegistry.get("python-3.12");
        assumeLanguageImageAvailable(python);

        String code = """
                arr = [i for i in range(500_000)]
                print(f"LEN={len(arr)} SUM={arr[-1]}")
                """;

        SimpleCodeExecutionResult result = codeExecutionService.run(SimpleCodeExecutionRequest.builder()
                .language(python)
                .code(code)
                .fileName("solution.py")
                .constraints(new CodeExecutionConstraints(3000L, 65536L))
                .build());

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.SUCCESS);
        assertThat(result.stdout().trim()).isEqualTo("LEN=500000 SUM=499999");
    }

    // =========================================================================
    // 2. Memory Limit Exceeded: Heap Violations (C++, Java, Python, Node.js)
    // =========================================================================

    @Test
    @Order(4)
    @DisplayName("C++ 23 std::bad_alloc Mapped to MEMORY_LIMIT_EXCEEDED")
    void testMemoryLimitExceededCppVector() {
        Language cpp = languageRegistry.get("cpp-23");
        assumeLanguageImageAvailable(cpp);

        // Attempting 200 MB vector allocation with 32 MB limit -> throws std::bad_alloc -> MEMORY_LIMIT_EXCEEDED
        String code = """
                #include <iostream>
                #include <vector>
                int main() {
                    std::vector<int> v(50 * 1024 * 1024); // 200 MB
                    std::cout << v.size() << "\\n";
                    return 0;
                }
                """;

        SimpleCodeExecutionResult result = codeExecutionService.run(SimpleCodeExecutionRequest.builder()
                .language(cpp)
                .code(code)
                .fileName("solution.cpp")
                .constraints(new CodeExecutionConstraints(3000L, 32768L)) // 32 MB limit
                .build());

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.MEMORY_LIMIT_EXCEEDED);
    }

    @Test
    @Order(5)
    @DisplayName("Java 21 JVM Heap OutOfMemoryError Mapped to MEMORY_LIMIT_EXCEEDED")
    void testMemoryLimitExceededJavaHeap() {
        Language java = languageRegistry.get("java-21");
        assumeLanguageImageAvailable(java);

        // 32 MB memory limit triggers proportional -Xmx24m. Code allocates 60 MB -> OutOfMemoryError
        String code = """
                import java.util.ArrayList;
                import java.util.List;

                public class Solution {
                    public static void main(String[] args) {
                        List<byte[]> list = new ArrayList<>();
                        for (int i = 0; i < 60; i++) {
                            list.add(new byte[1024 * 1024]); // 1MB per chunk
                        }
                        System.out.println("TOTAL=" + list.size());
                    }
                }
                """;

        SimpleCodeExecutionResult result = codeExecutionService.run(SimpleCodeExecutionRequest.builder()
                .language(java)
                .code(code)
                .fileName("Solution.java")
                .constraints(new CodeExecutionConstraints(4000L, 32768L))
                .build());

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.MEMORY_LIMIT_EXCEEDED);
    }

    @Test
    @Order(6)
    @DisplayName("Python 3.12 MemoryError Mapped to MEMORY_LIMIT_EXCEEDED")
    void testMemoryLimitExceededPythonList() {
        Language python = languageRegistry.get("python-3.12");
        assumeLanguageImageAvailable(python);

        // Allocating large list with 32 MB limit -> MemoryError -> MEMORY_LIMIT_EXCEEDED
        String code = """
                chunks = []
                for _ in range(500):
                    chunks.append("A" * (1024 * 1024)) # 1MB string
                print("LEN=", len(chunks))
                """;

        SimpleCodeExecutionResult result = codeExecutionService.run(SimpleCodeExecutionRequest.builder()
                .language(python)
                .code(code)
                .fileName("solution.py")
                .constraints(new CodeExecutionConstraints(3000L, 32768L))
                .build());

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.MEMORY_LIMIT_EXCEEDED);
    }

    @Test
    @Order(7)
    @DisplayName("JavaScript (Node.js 20) V8 Heap Out of Memory Mapped to MEMORY_LIMIT_EXCEEDED")
    void testMemoryLimitExceededNodeJsBuffer() {
        Language node = languageRegistry.get("javascript-node-20");
        assumeLanguageImageAvailable(node);

        // 32 MB limit sets --max-old-space-size=24. Allocating 50 MB on V8 heap -> heap OOM
        String code = """
                const chunks = [];
                for (let i = 0; i < 50; i++) {
                    chunks.push(new Array(1000000).fill(42));
                }
                console.log("ALLOCATED=" + chunks.length);
                """;

        SimpleCodeExecutionResult result = codeExecutionService.run(SimpleCodeExecutionRequest.builder()
                .language(node)
                .code(code)
                .fileName("solution.js")
                .constraints(new CodeExecutionConstraints(4000L, 32768L))
                .build());

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.MEMORY_LIMIT_EXCEEDED);
    }

    // =========================================================================
    // 3. Stack Exhaustion vs Heap MLE Classification
    // =========================================================================

    @Test
    @Order(8)
    @DisplayName("Stack Exhaustion via Infinite Recursion Mapped to RUNTIME_ERROR, NOT MLE (Java 21)")
    void testStackExhaustionDistinguishedFromMleJava() {
        Language java = languageRegistry.get("java-21");
        assumeLanguageImageAvailable(java);

        String code = """
                public class Solution {
                    public static void recurse(int depth) {
                        recurse(depth + 1);
                    }
                    public static void main(String[] args) {
                        recurse(0);
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
    @Order(9)
    @DisplayName("Stack Exhaustion via Recursion Limit Mapped to RUNTIME_ERROR, NOT MLE (Python 3.12)")
    void testStackExhaustionDistinguishedFromMlePython() {
        Language python = languageRegistry.get("python-3.12");
        assumeLanguageImageAvailable(python);

        String code = """
                def recurse(n):
                    return recurse(n + 1)
                recurse(0)
                """;

        SimpleCodeExecutionResult result = codeExecutionService.run(SimpleCodeExecutionRequest.builder()
                .language(python)
                .code(code)
                .fileName("solution.py")
                .constraints(new CodeExecutionConstraints(3000L, 131072L))
                .build());

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.RUNTIME_ERROR);
        assertThat(result.stderr()).contains("RecursionError");
    }

    @Test
    @Order(10)
    @DisplayName("Stack Exhaustion via Maximum Call Stack Mapped to RUNTIME_ERROR, NOT MLE (JavaScript Node 20)")
    void testStackExhaustionDistinguishedFromMleNode() {
        Language node = languageRegistry.get("javascript-node-20");
        assumeLanguageImageAvailable(node);

        String code = """
                function recurse(n) {
                    return recurse(n + 1);
                }
                recurse(0);
                """;

        SimpleCodeExecutionResult result = codeExecutionService.run(SimpleCodeExecutionRequest.builder()
                .language(node)
                .code(code)
                .fileName("solution.js")
                .constraints(new CodeExecutionConstraints(3000L, 131072L))
                .build());

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.RUNTIME_ERROR);
        assertThat(result.stderr()).contains("Maximum call stack size exceeded");
    }

    // =========================================================================
    // 4. Repeated Allocations with Garbage Collection Pressure
    // =========================================================================

    @Test
    @Order(11)
    @DisplayName("Repeated Short-Lived Allocations Pass When Garbage Collected (Java 21)")
    void testRepeatedAllocationsWithGarbageCollection() {
        Language java = languageRegistry.get("java-21");
        assumeLanguageImageAvailable(java);

        // Allocating 500 MB in chunks that are immediately dereferenced.
        // Under a 64 MB heap, GC easily reclaims and the program completes successfully.
        String code = """
                public class Solution {
                    public static void main(String[] args) {
                        long totalAllocated = 0;
                        for (int i = 0; i < 500; i++) {
                            byte[] chunk = new byte[1024 * 1024]; // 1MB
                            chunk[0] = (byte) i;
                            totalAllocated += chunk.length;
                        }
                        System.out.println("COLLECTED=" + (totalAllocated == 500L * 1024 * 1024));
                    }
                }
                """;

        SimpleCodeExecutionResult result = codeExecutionService.run(SimpleCodeExecutionRequest.builder()
                .language(java)
                .code(code)
                .fileName("Solution.java")
                .constraints(new CodeExecutionConstraints(4000L, 65536L)) // 64 MB limit
                .build());

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.SUCCESS);
        assertThat(result.stdout().trim()).isEqualTo("COLLECTED=true");
    }

    @Test
    @Order(12)
    @DisplayName("Gradually Retained Memory Breaches Threshold into MLE (C++ 23)")
    void testGraduallyRetainedMemoryBreachesThreshold() {
        Language cpp = languageRegistry.get("cpp-23");
        assumeLanguageImageAvailable(cpp);

        // Gradually allocating 1MB chunks and retaining them in vector until 32MB limit is breached
        String code = """
                #include <iostream>
                #include <vector>
                int main() {
                    std::vector<char*> ptrs;
                    for (int i = 0; i < 100; ++i) {
                        ptrs.push_back(new char[1024 * 1024]); // 1MB
                    }
                    std::cout << "RETAINED=" << ptrs.size() << "\\n";
                    return 0;
                }
                """;

        SimpleCodeExecutionResult result = codeExecutionService.run(SimpleCodeExecutionRequest.builder()
                .language(cpp)
                .code(code)
                .fileName("solution.cpp")
                .constraints(new CodeExecutionConstraints(3000L, 32768L)) // 32MB limit
                .build());

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.MEMORY_LIMIT_EXCEEDED);
    }
}

