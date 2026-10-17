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

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@DisplayName("Execution Time Limit & Asymptotic Complexity Integration Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ExecutionTimeLimitIntegrationTest extends BaseExecutionIT {

    // =========================================================================
    // 1. O(1), O(log n), O(log² n)
    // =========================================================================

    @Test
    @Order(1)
    @DisplayName("O(1) Constant Time - Math Closed Form Formula (C++ 23)")
    void testConstantTimeO1() {
        Language cpp = languageRegistry.get("cpp-23");
        assumeLanguageImageAvailable(cpp);

        String code = """
                #include <iostream>
                int main() {
                    long long n = 1000000000LL;
                    long long sum = n * (n + 1) / 2;
                    std::cout << sum << "\\n";
                    return 0;
                }
                """;

        SimpleCodeExecutionResult result = codeExecutionService.run(SimpleCodeExecutionRequest.builder()
                .language(cpp)
                .code(code)
                .fileName("solution.cpp")
                .constraints(new CodeExecutionConstraints(1000L, 65536L))
                .build());

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.SUCCESS);
        assertThat(result.stdout().trim()).isEqualTo("500000000500000000");
    }

    @Test
    @Order(2)
    @DisplayName("O(log n) Logarithmic Time - Binary Search (C++ 23)")
    void testLogarithmicTimeOlogN() {
        Language cpp = languageRegistry.get("cpp-23");
        assumeLanguageImageAvailable(cpp);

        String code = """
                #include <iostream>
                int main() {
                    long long low = 1, high = 1000000000000000000LL;
                    long long target = 777777777777777777LL;
                    long long steps = 0;
                    while (low <= high) {
                        steps++;
                        long long mid = low + (high - low) / 2;
                        if (mid == target) {
                            std::cout << "FOUND in " << steps << " steps\\n";
                            return 0;
                        } else if (mid < target) {
                            low = mid + 1;
                        } else {
                            high = mid - 1;
                        }
                    }
                    return 0;
                }
                """;

        SimpleCodeExecutionResult result = codeExecutionService.run(SimpleCodeExecutionRequest.builder()
                .language(cpp)
                .code(code)
                .fileName("solution.cpp")
                .constraints(new CodeExecutionConstraints(1000L, 65536L))
                .build());

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.SUCCESS);
        assertThat(result.stdout()).contains("FOUND in 60 steps");
    }

    @Test
    @Order(3)
    @DisplayName("O(log² n) Polylogarithmic Time - Binary Search on Monotonic Function (C++ 23)")
    void testPolylogarithmicTimeOlog2N() {
        Language cpp = languageRegistry.get("cpp-23");
        assumeLanguageImageAvailable(cpp);

        String code = """
                #include <iostream>
                // Outer binary search with inner logarithmic check -> O(log^2 N)
                long long countBits(long long n) {
                    long long c = 0;
                    while (n > 0) { c += (n & 1); n >>= 1; }
                    return c;
                }
                int main() {
                    long long low = 1, high = 1000000000000000000LL;
                    long long totalOperations = 0;
                    while (low <= high) {
                        long long mid = low + (high - low) / 2;
                        totalOperations += countBits(mid);
                        if (mid >= 500000000000000000LL) {
                            high = mid - 1;
                        } else {
                            low = mid + 1;
                        }
                    }
                    std::cout << "OPS=" << totalOperations << "\\n";
                    return 0;
                }
                """;

        SimpleCodeExecutionResult result = codeExecutionService.run(SimpleCodeExecutionRequest.builder()
                .language(cpp)
                .code(code)
                .fileName("solution.cpp")
                .constraints(new CodeExecutionConstraints(1000L, 65536L))
                .build());

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.SUCCESS);
        assertThat(result.stdout()).contains("OPS=");
    }

    // =========================================================================
    // 2. O(√n), O(n), O(n√n)
    // =========================================================================

    @Test
    @Order(4)
    @DisplayName("O(√n) Sublinear Primality Test PASS vs Naive O(n) TLE (C++ 23)")
    void testSqrtComplexityVsLinear() {
        Language cpp = languageRegistry.get("cpp-23");
        assumeLanguageImageAvailable(cpp);

        // O(√n) Primality test for N = 10^12 + 39 (Prime) -> 10^6 iterations -> PASS
        String sqrtCode = """
                #include <iostream>
                bool isPrime(long long n) {
                    if (n <= 1) return false;
                    for (long long i = 2; i * i <= n; i++) {
                        if (n % i == 0) return false;
                    }
                    return true;
                }
                int main() {
                    long long n = 1000000000039LL;
                    std::cout << (isPrime(n) ? "PRIME" : "COMPOSITE") << "\\n";
                    return 0;
                }
                """;

        SimpleCodeExecutionResult resPass = codeExecutionService.run(SimpleCodeExecutionRequest.builder()
                .language(cpp)
                .code(sqrtCode)
                .fileName("solution.cpp")
                .constraints(new CodeExecutionConstraints(1000L, 65536L))
                .build());

        assertThat(resPass.executionStatus()).isEqualTo(CodeExecutionStatus.SUCCESS);
        assertThat(resPass.stdout().trim()).isEqualTo("PRIME");

        // Naive O(n) Primality test -> 10^12 iterations -> TLE
        String linearCode = """
                #include <iostream>
                int main() {
                    long long n = 1000000000039LL;
                    volatile long long count = 0;
                    for (long long i = 2; i < n; i++) {
                        count += (n % i);
                    }
                    std::cout << count << "\\n";
                    return 0;
                }
                """;

        SimpleCodeExecutionResult resTle = codeExecutionService.run(SimpleCodeExecutionRequest.builder()
                .language(cpp)
                .code(linearCode)
                .fileName("solution.cpp")
                .constraints(new CodeExecutionConstraints(1000L, 65536L))
                .build());

        assertThat(resTle.executionStatus()).isEqualTo(CodeExecutionStatus.TIME_LIMIT_EXCEEDED);
    }

    @Test
    @Order(5)
    @DisplayName("O(n) Linear Time Scaling - Small Input Pass vs Massive Input TLE (Python 3.12)")
    void testLinearComplexityScaling() {
        Language python = languageRegistry.get("python-3.12");
        assumeLanguageImageAvailable(python);

        // Small N = 1,000,000 -> PASS
        String smallCode = """
                n = 1_000_000
                total = sum(range(n))
                print(f"SUM={total}")
                """;

        SimpleCodeExecutionResult resPass = codeExecutionService.run(SimpleCodeExecutionRequest.builder()
                .language(python)
                .code(smallCode)
                .fileName("solution.py")
                .constraints(new CodeExecutionConstraints(3000L, 262144L))
                .build());

        assertThat(resPass.executionStatus()).isEqualTo(CodeExecutionStatus.SUCCESS);
        assertThat(resPass.stdout()).contains("SUM=499999500000");

        // Massive N = 100,000,000 in Python loop with 1000ms limit -> TLE
        String massiveCode = """
                count = 0
                for i in range(100_000_000):
                    count += 1
                print(count)
                """;

        SimpleCodeExecutionResult resTle = codeExecutionService.run(SimpleCodeExecutionRequest.builder()
                .language(python)
                .code(massiveCode)
                .fileName("solution.py")
                .constraints(new CodeExecutionConstraints(1000L, 262144L))
                .build());

        assertThat(resTle.executionStatus()).isEqualTo(CodeExecutionStatus.TIME_LIMIT_EXCEEDED);
    }

    @Test
    @Order(6)
    @DisplayName("O(n√n) Sieve / Divisor Counting (C++ 23)")
    void testSqrtDecompositionComplexity() {
        Language cpp = languageRegistry.get("cpp-23");
        assumeLanguageImageAvailable(cpp);

        // N = 200,000 numbers, computing divisors up to sqrt(i)
        String code = """
                #include <iostream>
                int main() {
                    int n = 200000;
                    long long totalDivisors = 0;
                    for (int i = 1; i <= n; i++) {
                        int count = 0;
                        for (int d = 1; d * d <= i; d++) {
                            if (i % d == 0) {
                                count += (d * d == i) ? 1 : 2;
                            }
                        }
                        totalDivisors += count;
                    }
                    std::cout << "TOTAL=" << totalDivisors << "\\n";
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
        assertThat(result.stdout()).contains("TOTAL=");
    }

    // =========================================================================
    // 3. O(n log n), O(n log² n)
    // =========================================================================

    @Test
    @Order(7)
    @DisplayName("O(n log n) Dual-Pivot Quicksort (Java 21)")
    void testLinearithmicComplexityQuicksort() {
        Language java = languageRegistry.get("java-21");
        assumeLanguageImageAvailable(java);

        String code = """
                import java.util.Arrays;
                import java.util.Random;

                public class Solution {
                    public static void main(String[] args) {
                        int n = 100_000;
                        int[] arr = new int[n];
                        Random rng = new Random(42);
                        for (int i = 0; i < n; i++) arr[i] = rng.nextInt();
                        Arrays.sort(arr);
                        System.out.println("SORTED=" + (arr[0] <= arr[n - 1]));
                    }
                }
                """;

        SimpleCodeExecutionResult result = codeExecutionService.run(SimpleCodeExecutionRequest.builder()
                .language(java)
                .code(code)
                .fileName("Solution.java")
                .constraints(new CodeExecutionConstraints(2000L, 262144L))
                .build());

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.SUCCESS);
        assertThat(result.stdout().trim()).isEqualTo("SORTED=true");
    }

    @Test
    @Order(8)
    @DisplayName("O(n log² n) Fenwick Tree Point Updates with Range Binary Search (C++ 23)")
    void testNLog2NComplexity() {
        Language cpp = languageRegistry.get("cpp-23");
        assumeLanguageImageAvailable(cpp);

        String code = """
                #include <iostream>
                #include <vector>

                int main() {
                    int n = 100000;
                    std::vector<int> bit(n + 1, 0);
                    // O(n log n) updates
                    for (int i = 1; i <= n; i++) {
                        for (int idx = i; idx <= n; idx += idx & -idx) {
                            bit[idx] += 1;
                        }
                    }
                    // Binary search queries O(n log^2 n)
                    long long total = 0;
                    for (int i = 1; i <= 20000; i++) {
                        int low = 1, high = n;
                        while (low <= high) {
                            int mid = low + (high - low) / 2;
                            int sum = 0;
                            for (int idx = mid; idx > 0; idx -= idx & -idx) sum += bit[idx];
                            if (sum >= i) high = mid - 1;
                            else low = mid + 1;
                        }
                        total += low;
                    }
                    std::cout << "RES=" << total << "\\n";
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
        assertThat(result.stdout()).contains("RES=");
    }

    // =========================================================================
    // 4. O(n²), O(n² log n), O(n³), O(n⁴), O(nᵏ)
    // =========================================================================

    @Test
    @Order(9)
    @DisplayName("O(n²) Bubble Sort - Small Input Pass vs Large Input TLE (Java 21)")
    void testQuadraticComplexityBubbleSort() {
        Language java = languageRegistry.get("java-21");
        assumeLanguageImageAvailable(java);

        // Small N = 500 -> O(250,000) -> PASS
        String smallSort = """
                public class Solution {
                    public static void main(String[] args) {
                        int n = 500;
                        int[] a = new int[n];
                        for (int i = 0; i < n; i++) a[i] = n - i;
                        for (int i = 0; i < n; i++) {
                            for (int j = 0; j < n - 1; j++) {
                                if (a[j] > a[j + 1]) {
                                    int t = a[j]; a[j] = a[j + 1]; a[j + 1] = t;
                                }
                            }
                        }
                        System.out.println("SORTED=" + (a[0] == 1 && a[n - 1] == n));
                    }
                }
                """;

        SimpleCodeExecutionResult resPass = codeExecutionService.run(SimpleCodeExecutionRequest.builder()
                .language(java)
                .code(smallSort)
                .fileName("Solution.java")
                .constraints(new CodeExecutionConstraints(2000L, 262144L))
                .build());

        assertThat(resPass.executionStatus()).isEqualTo(CodeExecutionStatus.SUCCESS);
        assertThat(resPass.stdout().trim()).isEqualTo("SORTED=true");

        // Large N = 50,000 -> 1.25 * 10^9 array swaps -> TLE
        String largeSort = """
                public class Solution {
                    public static void main(String[] args) {
                        int n = 50_000;
                        int[] a = new int[n];
                        for (int i = 0; i < n; i++) a[i] = n - i;
                        for (int i = 0; i < n; i++) {
                            for (int j = 0; j < n - 1; j++) {
                                if (a[j] > a[j + 1]) {
                                    int t = a[j]; a[j] = a[j + 1]; a[j + 1] = t;
                                }
                            }
                        }
                        System.out.println(a[0]);
                    }
                }
                """;

        SimpleCodeExecutionResult resTle = codeExecutionService.run(SimpleCodeExecutionRequest.builder()
                .language(java)
                .code(largeSort)
                .fileName("Solution.java")
                .constraints(new CodeExecutionConstraints(1000L, 262144L))
                .build());

        assertThat(resTle.executionStatus()).isEqualTo(CodeExecutionStatus.TIME_LIMIT_EXCEEDED);
    }

    @Test
    @Order(10)
    @DisplayName("O(n² log n) 2D Coordinate Sweep Line (C++ 23)")
    void testN2LogNComplexity() {
        Language cpp = languageRegistry.get("cpp-23");
        assumeLanguageImageAvailable(cpp);

        String code = """
                #include <iostream>
                #include <vector>
                #include <algorithm>

                int main() {
                    int n = 1000;
                    std::vector<int> row(n);
                    long long total = 0;
                    for (int i = 0; i < n; i++) {
                        for (int j = 0; j < n; j++) row[j] = (i * j) % 997;
                        std::sort(row.begin(), row.end());
                        total += row[n / 2];
                    }
                    std::cout << "MEDIANS=" << total << "\\n";
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
        assertThat(result.stdout()).contains("MEDIANS=");
    }

    @Test
    @Order(11)
    @DisplayName("O(n³) Cubic Matrix Multiplication - Small Pass vs Large TLE (C++ 23)")
    void testCubicComplexityMatrixMultiplication() {
        Language cpp = languageRegistry.get("cpp-23");
        assumeLanguageImageAvailable(cpp);

        // Small N = 40 -> 64,000 ops -> PASS
        String smallMatMul = """
                #include <iostream>
                #include <vector>
                int main() {
                    int n = 40;
                    std::vector<std::vector<int>> A(n, std::vector<int>(n, 1));
                    std::vector<std::vector<int>> C(n, std::vector<int>(n, 0));
                    for (int i = 0; i < n; ++i)
                        for (int j = 0; j < n; ++j)
                            for (int k = 0; k < n; ++k)
                                C[i][j] += A[i][k] * A[k][j];
                    std::cout << "C[0][0]=" << C[0][0] << "\\n";
                    return 0;
                }
                """;

        SimpleCodeExecutionResult resPass = codeExecutionService.run(SimpleCodeExecutionRequest.builder()
                .language(cpp)
                .code(smallMatMul)
                .fileName("solution.cpp")
                .constraints(new CodeExecutionConstraints(1000L, 65536L))
                .build());

        assertThat(resPass.executionStatus()).isEqualTo(CodeExecutionStatus.SUCCESS);
        assertThat(resPass.stdout().trim()).isEqualTo("C[0][0]=40");

        // Large N = 1,500 -> 3.375 * 10^9 ops -> TLE
        String largeMatMul = """
                #include <iostream>
                int main() {
                    int n = 2500;
                    volatile long long ops = 0;
                    for (int i = 0; i < n; ++i)
                        for (int j = 0; j < n; ++j)
                            for (int k = 0; k < n; ++k)
                                ops++;
                    std::cout << ops << "\\n";
                    return 0;
                }
                """;

        SimpleCodeExecutionResult resTle = codeExecutionService.run(SimpleCodeExecutionRequest.builder()
                .language(cpp)
                .code(largeMatMul)
                .fileName("solution.cpp")
                .constraints(new CodeExecutionConstraints(1000L, 131072L))
                .build());

        assertThat(resTle.executionStatus()).isEqualTo(CodeExecutionStatus.TIME_LIMIT_EXCEEDED);
    }

    @Test
    @Order(12)
    @DisplayName("O(n⁴) 4-Nested Loops Small Pass vs O(n⁵) 5-Nested Loops TLE (C 17)")
    void testHigherPolynomialComplexity() {
        Language c = languageRegistry.get("c-17");
        assumeLanguageImageAvailable(c);

        // Small O(n^4) N = 20 (20^4 = 160,000) -> PASS
        String smallN4 = """
                #include <stdio.h>
                int main() {
                    int n = 20;
                    long long count = 0;
                    for (int a = 0; a < n; a++)
                        for (int b = 0; b < n; b++)
                            for (int c = 0; c < n; c++)
                                for (int d = 0; d < n; d++)
                                     count++;
                    printf("%lld\\n", count);
                    return 0;
                }
                """;

        SimpleCodeExecutionResult resPass = codeExecutionService.run(SimpleCodeExecutionRequest.builder()
                .language(c)
                .code(smallN4)
                .fileName("main.c")
                .constraints(new CodeExecutionConstraints(1000L, 65536L))
                .build());

        assertThat(resPass.executionStatus()).isEqualTo(CodeExecutionStatus.SUCCESS);
        assertThat(resPass.stdout().trim()).isEqualTo("160000");

        // O(n^5) N = 100 (100^5 = 10^10 volatile ops) -> TLE
        String largeN5 = """
                #include <stdio.h>
                int main() {
                    int n = 100;
                    volatile long long count = 0;
                    for (int a = 0; a < n; a++)
                        for (int b = 0; b < n; b++)
                            for (int c = 0; c < n; c++)
                                for (int d = 0; d < n; d++)
                                    for (int e = 0; e < n; e++)
                                        count++;
                    printf("%lld\\n", count);
                    return 0;
                }
                """;

        SimpleCodeExecutionResult resTle = codeExecutionService.run(SimpleCodeExecutionRequest.builder()
                .language(c)
                .code(largeN5)
                .fileName("main.c")
                .constraints(new CodeExecutionConstraints(1000L, 65536L))
                .build());

        assertThat(resTle.executionStatus()).isEqualTo(CodeExecutionStatus.TIME_LIMIT_EXCEEDED);
    }

    // =========================================================================
    // 5. O(2ⁿ), O(3ⁿ), O(n · 2ⁿ), O(n!), O(nⁿ)
    // =========================================================================

    @Test
    @Order(13)
    @DisplayName("O(2ⁿ) Exponential Time - Recursive Fibonacci Small Pass vs Large TLE (JavaScript Node 20)")
    void testExponentialComplexityNodeJs() {
        Language node = languageRegistry.get("javascript-node-20");
        assumeLanguageImageAvailable(node);

        // Small N = 15 (2^15 calls) -> PASS
        String smallFib = """
                function fib(n) {
                    if (n <= 1) return n;
                    return fib(n - 1) + fib(n - 2);
                }
                console.log(fib(15));
                """;

        SimpleCodeExecutionResult resSmall = codeExecutionService.run(SimpleCodeExecutionRequest.builder()
                .language(node)
                .code(smallFib)
                .fileName("solution.js")
                .constraints(new CodeExecutionConstraints(2000L, 262144L))
                .build());

        assertThat(resSmall.executionStatus()).isEqualTo(CodeExecutionStatus.SUCCESS);
        assertThat(resSmall.stdout().trim()).isEqualTo("610");

        // Large N = 45 (~2*10^9 recursive calls) -> TLE
        String largeFib = """
                function fib(n) {
                    if (n <= 1) return n;
                    return fib(n - 1) + fib(n - 2);
                }
                console.log(fib(45));
                """;

        SimpleCodeExecutionResult resTle = codeExecutionService.run(SimpleCodeExecutionRequest.builder()
                .language(node)
                .code(largeFib)
                .fileName("solution.js")
                .constraints(new CodeExecutionConstraints(1000L, 262144L))
                .build());

        assertThat(resTle.executionStatus()).isEqualTo(CodeExecutionStatus.TIME_LIMIT_EXCEEDED);
    }

    @Test
    @Order(14)
    @DisplayName("O(n · 2ⁿ) Bitmask Subset DP - Small Pass vs Large TLE (C++ 23)")
    void testBitmaskSubsetComplexity() {
        Language cpp = languageRegistry.get("cpp-23");
        assumeLanguageImageAvailable(cpp);

        // N = 12 -> 12 * 4096 = 49,152 operations -> PASS
        String smallBitmask = """
                #include <iostream>
                int main() {
                    int n = 12;
                    long long total = 0;
                    for (int mask = 0; mask < (1 << n); mask++) {
                        for (int i = 0; i < n; i++) {
                            if (mask & (1 << i)) total++;
                        }
                    }
                    std::cout << "BITS=" << total << "\\n";
                    return 0;
                }
                """;

        SimpleCodeExecutionResult resPass = codeExecutionService.run(SimpleCodeExecutionRequest.builder()
                .language(cpp)
                .code(smallBitmask)
                .fileName("solution.cpp")
                .constraints(new CodeExecutionConstraints(1000L, 65536L))
                .build());

        assertThat(resPass.executionStatus()).isEqualTo(CodeExecutionStatus.SUCCESS);
        assertThat(resPass.stdout().trim()).isEqualTo("BITS=24576");

        // N = 26 -> 26 * 67,108,864 = 1.74 * 10^9 operations -> TLE
        String largeBitmask = """
                #include <iostream>
                int main() {
                    int n = 26;
                    volatile long long total = 0;
                    for (int mask = 0; mask < (1 << n); mask++) {
                        for (int i = 0; i < n; i++) {
                            if (mask & (1 << i)) total++;
                        }
                    }
                    std::cout << total << "\\n";
                    return 0;
                }
                """;

        SimpleCodeExecutionResult resTle = codeExecutionService.run(SimpleCodeExecutionRequest.builder()
                .language(cpp)
                .code(largeBitmask)
                .fileName("solution.cpp")
                .constraints(new CodeExecutionConstraints(1000L, 65536L))
                .build());

        assertThat(resTle.executionStatus()).isEqualTo(CodeExecutionStatus.TIME_LIMIT_EXCEEDED);
    }

    @Test
    @Order(15)
    @DisplayName("O(n!) Factorial Time - Permutations Small Pass vs Large TLE (C++ 23)")
    void testFactorialComplexityCpp() {
        Language cpp = languageRegistry.get("cpp-23");
        assumeLanguageImageAvailable(cpp);

        // Small N = 8 (8! = 40,320 permutations) -> PASS
        String smallPerm = """
                #include <iostream>
                #include <vector>
                #include <numeric>
                #include <algorithm>
                int main() {
                    std::vector<int> p(8);
                    std::iota(p.begin(), p.end(), 0);
                    long long count = 0;
                    do { count++; } while (std::next_permutation(p.begin(), p.end()));
                    std::cout << "PERMS=" << count << "\\n";
                    return 0;
                }
                """;

        SimpleCodeExecutionResult resSmall = codeExecutionService.run(SimpleCodeExecutionRequest.builder()
                .language(cpp)
                .code(smallPerm)
                .fileName("solution.cpp")
                .constraints(new CodeExecutionConstraints(1000L, 65536L))
                .build());

        assertThat(resSmall.executionStatus()).isEqualTo(CodeExecutionStatus.SUCCESS);
        assertThat(resSmall.stdout().trim()).isEqualTo("PERMS=40320");

        // Large N = 14 (14! = 8.7 * 10^10 permutations) -> bounded TLE
        String largePerm = """
                #include <iostream>
                #include <vector>
                #include <numeric>
                #include <algorithm>
                int main() {
                    std::vector<int> p(14);
                    std::iota(p.begin(), p.end(), 0);
                    volatile long long count = 0;
                    do { count++; } while (std::next_permutation(p.begin(), p.end()));
                    std::cout << count << "\\n";
                    return 0;
                }
                """;

        SimpleCodeExecutionResult resTle = codeExecutionService.run(SimpleCodeExecutionRequest.builder()
                .language(cpp)
                .code(largePerm)
                .fileName("solution.cpp")
                .constraints(new CodeExecutionConstraints(1000L, 65536L))
                .build());

        assertThat(resTle.executionStatus()).isEqualTo(CodeExecutionStatus.TIME_LIMIT_EXCEEDED);
    }

    // =========================================================================
    // 6. Infinite Loops, Threshold Boundaries & Consistency
    // =========================================================================

    @Test
    @Order(16)
    @DisplayName("Infinite while(true) loop terminated by Isolate timer (C++ 23)")
    void testInfiniteLoopTerminatedCleanly() {
        Language cpp = languageRegistry.get("cpp-23");
        assumeLanguageImageAvailable(cpp);

        String code = "int main() { while (true) {} return 0; }";

        SimpleCodeExecutionResult result = codeExecutionService.run(SimpleCodeExecutionRequest.builder()
                .language(cpp)
                .code(code)
                .fileName("solution.cpp")
                .constraints(new CodeExecutionConstraints(1000L, 65536L))
                .build());

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.TIME_LIMIT_EXCEEDED);
    }

    @Test
    @Order(17)
    @DisplayName("Threshold Boundary Test: 600ms PASS vs 1400ms TLE under 1000ms limit (Python 3.12)")
    void testThresholdBoundaryBehavior() {
        Language python = languageRegistry.get("python-3.12");
        assumeLanguageImageAvailable(python);

        // Passing boundary: ~5M iterations in Python takes ~0.4-0.6s -> PASS under 1000ms limit
        String passCode = """
                count = 0
                for i in range(5_000_000):
                    count += 1
                print("BOUND_PASS")
                """;

        SimpleCodeExecutionResult resPass = codeExecutionService.run(SimpleCodeExecutionRequest.builder()
                .language(python)
                .code(passCode)
                .fileName("solution.py")
                .constraints(new CodeExecutionConstraints(1000L, 262144L))
                .build());

        assertThat(resPass.executionStatus()).isEqualTo(CodeExecutionStatus.SUCCESS);
        assertThat(resPass.stdout().trim()).isEqualTo("BOUND_PASS");

        // Failing boundary: ~25M iterations in Python takes ~2.0s -> TLE under 1000ms limit
        String failCode = """
                count = 0
                for i in range(25_000_000):
                    count += 1
                print("BOUND_FAIL")
                """;

        SimpleCodeExecutionResult resFail = codeExecutionService.run(SimpleCodeExecutionRequest.builder()
                .language(python)
                .code(failCode)
                .fileName("solution.py")
                .constraints(new CodeExecutionConstraints(1000L, 262144L))
                .build());

        assertThat(resFail.executionStatus()).isEqualTo(CodeExecutionStatus.TIME_LIMIT_EXCEEDED);
    }

    @Test
    @Order(18)
    @DisplayName("Repeated Runs Consistency - Verify Zero Flakiness Across Repeated Invocations (C++ 23)")
    void testRepeatedRunsConsistency() {
        Language cpp = languageRegistry.get("cpp-23");
        assumeLanguageImageAvailable(cpp);

        String code = """
                #include <iostream>
                int main() {
                    long long sum = 0;
                    for (int i = 0; i < 5000000; i++) sum += i;
                    std::cout << "SUM=" << sum << "\\n";
                    return 0;
                }
                """;

        List<SimpleCodeExecutionResult> results = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            SimpleCodeExecutionResult res = codeExecutionService.run(SimpleCodeExecutionRequest.builder()
                    .language(cpp)
                    .code(code)
                    .fileName("solution.cpp")
                    .constraints(new CodeExecutionConstraints(1000L, 65536L))
                    .build());
            results.add(res);
        }

        for (SimpleCodeExecutionResult r : results) {
            assertThat(r.executionStatus()).isEqualTo(CodeExecutionStatus.SUCCESS);
            assertThat(r.stdout()).contains("SUM=12499997500000");
        }
    }
}

