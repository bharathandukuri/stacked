package com.bharath.stacked.modules.coding_problem.integration;

import com.bharath.stacked.AbstractIntegrationTest;
import com.bharath.stacked.modules.coding_problem.dto.request.CreateCodingProblemRequest;
import com.bharath.stacked.modules.coding_problem.dto.request.ExecutionConstraintRequest;
import com.bharath.stacked.modules.coding_problem.dto.request.TestcaseRequest;
import com.bharath.stacked.modules.coding_problem.enums.CodingProblemDifficulty;
import com.bharath.stacked.modules.coding_problem.enums.CodingProblemType;
import com.bharath.stacked.modules.coding_problem.judge.CodingProblemJudgeService;
import com.bharath.stacked.modules.coding_problem.model.CodingProblem;
import com.bharath.stacked.modules.coding_problem.repository.CodingProblemRepository;
import com.bharath.stacked.modules.coding_problem.service.CodingProblemService;
import com.bharath.stacked.modules.execution.dto.ManagedContainer;
import com.bharath.stacked.modules.execution.registry.ManagedContainerRegistry;
import com.bharath.stacked.modules.execution.service.CodeExecutionService;
import com.bharath.stacked.modules.execution.service.DockerExecutionService;
import com.bharath.stacked.modules.language.Language;
import com.bharath.stacked.modules.language.registry.LanguageRegistry;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
public abstract class BaseCodingProblemLiveIT extends AbstractIntegrationTest {

    @DynamicPropertySource
    static void enableDockerExecution(DynamicPropertyRegistry registry) {
        registry.add("stacked.execution.docker.enabled", () -> "true");
    }

    @Autowired
    protected CodingProblemService codingProblemService;

    @Autowired
    protected CodingProblemRepository codingProblemRepository;

    @Autowired
    protected CodingProblemJudgeService codingProblemJudgeService;

    @Autowired
    protected CodeExecutionService codeExecutionService;

    @Autowired
    protected LanguageRegistry languageRegistry;

    @Autowired
    protected DockerExecutionService dockerExecutionService;

    @Autowired
    protected ManagedContainerRegistry containerRegistry;

    @Autowired
    protected MongoTemplate mongoTemplate;

    protected final List<String> createdProblemIds = new CopyOnWriteArrayList<>();

    @AfterEach
    void tearDownProblemsAndVerifyNoContainerLeaks() {
        for (String id : createdProblemIds) {
            try {
                codingProblemService.delete(id);
                log.info("Cleaned up integration test coding problem [{}]", id);
            } catch (Exception e) {
                try {
                    codingProblemRepository.deleteById(id);
                } catch (Exception ignored) {
                }
            }
        }
        createdProblemIds.clear();

        if (containerRegistry != null) {
            List<ManagedContainer> lingering = containerRegistry.findCreatedBefore(Instant.now().plusSeconds(60));
            assertThat(lingering)
                    .as("No Docker execution containers should linger in Redis after judging completes")
                    .isEmpty();
        }
    }

    protected void assumeImageAvailable(String languageId) {
        Language lang = languageRegistry.get(languageId);
        Assumptions.assumeTrue(lang != null, "Language [" + languageId + "] not found in registry.");
        boolean exists = dockerExecutionService.isImageExists(lang.dockerImageDetails());
        Assumptions.assumeTrue(exists, "Docker image [" + lang.dockerImageDetails().reference() + "] not available locally.");
    }

    protected String randomSlug(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    // =========================================================================
    // Generic Problem Builder: Two Sum (Array Processing)
    // =========================================================================
    protected CodingProblem createTwoSumProblem(String slug) {
        CreateCodingProblemRequest req = new CreateCodingProblemRequest(
                "Two Sum Integration",
                slug,
                CodingProblemType.GENERIC,
                CodingProblemDifficulty.EASY,
                List.of("python-3.12", "java-21", "javascript-node-20", "cpp-23"),
                "Given an array of integers `nums` and an integer `target`, return indices of the two numbers such that they add up to `target`.",
                List.of("Array", "Hash Table"),
                List.of("Use a hash map for O(n) lookup"),
                Map.of(
                        "python-3.12", "import sys\n\ndef solve():\n    pass\n\nsolve()\n",
                        "java-21", "import java.util.*;\n\npublic class Solution {\n    public static void main(String[] args) {\n    }\n}\n",
                        "javascript-node-20", "const fs = require('fs');\nconst input = fs.readFileSync('/dev/stdin', 'utf-8');\n",
                        "cpp-23", "#include <iostream>\nusing namespace std;\n\nint main() {\n    return 0;\n}\n"
                ),
                "python-3.12",
                """
                import sys

                def solve():
                    raw = sys.stdin.read().split()
                    if not raw:
                        return
                    n = int(raw[0])
                    target = int(raw[1])
                    nums = [int(x) for x in raw[2:2+n]]
                    seen = {}
                    for i, num in enumerate(nums):
                        diff = target - num
                        if diff in seen:
                            print(f"{seen[diff]} {i}")
                            return
                        seen[num] = i

                solve()
                """,
                false,
                null,
                null,
                List.of(
                        new TestcaseRequest("4 9\n2 7 11 15", null),
                        new TestcaseRequest("3 6\n3 2 4", null)
                ),
                List.of(
                        new TestcaseRequest("4 0\n-3 4 3 90", null),
                        new TestcaseRequest("2 6\n3 3", null),
                        new TestcaseRequest("5 100\n10 20 30 40 60", null)
                ),
                Map.of(
                        "python-3.12", new ExecutionConstraintRequest(2000L, 262144L),
                        "java-21", new ExecutionConstraintRequest(3000L, 262144L),
                        "javascript-node-20", new ExecutionConstraintRequest(2000L, 262144L),
                        "cpp-23", new ExecutionConstraintRequest(2000L, 262144L)
                ),
                null,
                null,
                null,
                null,
                List.of()
        );

        CodingProblem problem = codingProblemService.create(req);
        createdProblemIds.add(problem.getId());
        return problem;
    }

    // =========================================================================
    // Generic Problem Builder: Two Sum with Multiple Valid Answers (Verification Script)
    // =========================================================================
    protected CodingProblem createTwoSumWithVerifierProblem(String slug) {
        String verifierScript = """
                import sys

                raw = sys.stdin.read()
                parts = raw.split("---STACKED_INPUT_END---")
                if len(parts) != 2:
                    sys.exit(1)

                stdin_data = parts[0].strip().split()
                cand_stdout = parts[1].strip().split()

                if len(cand_stdout) != 2:
                    assert False, f"Expected 2 indices, got {cand_stdout}"

                try:
                    i = int(cand_stdout[0])
                    j = int(cand_stdout[1])
                except ValueError:
                    assert False, "Indices must be valid integers"

                n = int(stdin_data[0])
                target = int(stdin_data[1])
                nums = [int(x) for x in stdin_data[2:2+n]]

                assert 0 <= i < n, f"Index i={i} out of bounds"
                assert 0 <= j < n, f"Index j={j} out of bounds"
                assert i != j, "Indices must be distinct"
                assert nums[i] + nums[j] == target, f"nums[{i}] ({nums[i]}) + nums[{j}] ({nums[j]}) != {target}"
                """;

        CreateCodingProblemRequest req = new CreateCodingProblemRequest(
                "Two Sum Alternate Answers",
                slug,
                CodingProblemType.GENERIC,
                CodingProblemDifficulty.MEDIUM,
                List.of("python-3.12", "java-21", "javascript-node-20", "cpp-23"),
                "Return ANY valid pair of indices that add up to target.",
                List.of("Array"),
                List.of(),
                Map.of("python-3.12", "# solution\n"),
                "python-3.12",
                """
                import sys

                raw = sys.stdin.read().split()
                if raw:
                    n = int(raw[0])
                    target = int(raw[1])
                    nums = [int(x) for x in raw[2:2+n]]
                    seen = {}
                    for i, num in enumerate(nums):
                        diff = target - num
                        if diff in seen:
                            print(f"{seen[diff]} {i}")
                            break
                        seen[num] = i
                """,
                true,
                "python-3.12",
                verifierScript,
                List.of(
                        new TestcaseRequest("4 10\n2 8 8 2", null) // multiple valid pairs: (0, 1), (0, 2), (1, 3), (2, 3)
                ),
                List.of(
                        new TestcaseRequest("4 4\n2 2 2 2", null) // multiple pairs
                ),
                Map.of(
                        "python-3.12", new ExecutionConstraintRequest(2000L, 262144L),
                        "java-21", new ExecutionConstraintRequest(3000L, 262144L),
                        "javascript-node-20", new ExecutionConstraintRequest(2000L, 262144L),
                        "cpp-23", new ExecutionConstraintRequest(2000L, 262144L)
                ),
                null,
                null,
                null,
                null,
                List.of()
        );

        CodingProblem problem = codingProblemService.create(req);
        createdProblemIds.add(problem.getId());
        return problem;
    }

    // =========================================================================
    // Database Problem Builder: Employee Department Highest Salary (SQLite 3)
    // =========================================================================
    protected CodingProblem createEmployeeSalarySqliteProblem(String slug) {
        String initSqlSample1 = """
                CREATE TABLE department (id INTEGER PRIMARY KEY, name TEXT);
                CREATE TABLE employee (id INTEGER PRIMARY KEY, name TEXT, salary INTEGER, department_id INTEGER);

                INSERT INTO department VALUES (1, 'IT'), (2, 'Sales');
                INSERT INTO employee VALUES (1, 'Joe', 85000, 1);
                INSERT INTO employee VALUES (2, 'Henry', 80000, 2);
                INSERT INTO employee VALUES (3, 'Sam', 60000, 2);
                INSERT INTO employee VALUES (4, 'Max', 90000, 1);
                """;

        String initSqlSample2Duplicates = """
                CREATE TABLE department (id INTEGER PRIMARY KEY, name TEXT);
                CREATE TABLE employee (id INTEGER PRIMARY KEY, name TEXT, salary INTEGER, department_id INTEGER);

                INSERT INTO department VALUES (1, 'IT');
                INSERT INTO employee VALUES (1, 'Alice', 90000, 1);
                INSERT INTO employee VALUES (2, 'Bob', 90000, 1);
                INSERT INTO employee VALUES (3, 'Charlie', 70000, 1);
                """;

        String initSqlHiddenEmpty = """
                CREATE TABLE department (id INTEGER PRIMARY KEY, name TEXT);
                CREATE TABLE employee (id INTEGER PRIMARY KEY, name TEXT, salary INTEGER, department_id INTEGER);
                """;

        String initSqlHiddenNullDept = """
                CREATE TABLE department (id INTEGER PRIMARY KEY, name TEXT);
                CREATE TABLE employee (id INTEGER PRIMARY KEY, name TEXT, salary INTEGER, department_id INTEGER);

                INSERT INTO department VALUES (1, 'Finance');
                INSERT INTO employee VALUES (1, 'Ghost', 100000, NULL);
                INSERT INTO employee VALUES (2, 'Dan', 75000, 1);
                """;

        String refSolution = """
                SELECT d.name AS Department, e.name AS Employee, e.salary AS Salary
                FROM employee e
                JOIN department d ON e.department_id = d.id
                WHERE e.salary = (
                    SELECT MAX(e2.salary)
                    FROM employee e2
                    WHERE e2.department_id = e.department_id
                )
                ORDER BY Department ASC, Employee ASC;
                """;

        CreateCodingProblemRequest req = new CreateCodingProblemRequest(
                "Department Highest Salary",
                slug,
                CodingProblemType.DATABASE,
                CodingProblemDifficulty.MEDIUM,
                List.of("sqlite-3"),
                "Find employees who have the highest salary in each of the departments.",
                List.of("Database", "SQL"),
                List.of(),
                Map.of("sqlite-3", "-- Write your SQL query below\n"),
                "sqlite-3",
                refSolution,
                false,
                null,
                null,
                List.of(
                        new TestcaseRequest(null, initSqlSample1),
                        new TestcaseRequest(null, initSqlSample2Duplicates)
                ),
                List.of(
                        new TestcaseRequest(null, initSqlHiddenEmpty),
                        new TestcaseRequest(null, initSqlHiddenNullDept)
                ),
                Map.of("sqlite-3", new ExecutionConstraintRequest(5000L, 262144L)),
                null,
                null,
                null,
                null,
                List.of()
        );

        CodingProblem problem = codingProblemService.create(req);
        createdProblemIds.add(problem.getId());
        return problem;
    }

    // =========================================================================
    // Database Problem Builder: Customer Orders Aggregation (PostgreSQL 16)
    // =========================================================================
    protected CodingProblem createCustomerOrdersPostgresProblem(String slug) {
        String initSqlSample = """
                CREATE TABLE customers (id SERIAL PRIMARY KEY, name VARCHAR(50) NOT NULL);
                CREATE TABLE orders (id SERIAL PRIMARY KEY, customer_id INT NOT NULL, amount NUMERIC(10,2) NOT NULL, status VARCHAR(20) NOT NULL);

                INSERT INTO customers (id, name) VALUES (1, 'Alice'), (2, 'Bob'), (3, 'Charlie');
                INSERT INTO orders (customer_id, amount, status) VALUES (1, 100.50, 'COMPLETED'), (1, 200.00, 'COMPLETED'), (2, 50.00, 'CANCELLED');
                """;

        String initSqlHidden = """
                CREATE TABLE customers (id SERIAL PRIMARY KEY, name VARCHAR(50) NOT NULL);
                CREATE TABLE orders (id SERIAL PRIMARY KEY, customer_id INT NOT NULL, amount NUMERIC(10,2) NOT NULL, status VARCHAR(20) NOT NULL);

                INSERT INTO customers (id, name) VALUES (1, 'EmptyCust');
                """;

        String refSolution = """
                SELECT c.name, COALESCE(SUM(o.amount), 0) AS total_spent
                FROM customers c
                LEFT JOIN orders o ON c.id = o.customer_id AND o.status = 'COMPLETED'
                GROUP BY c.id, c.name
                ORDER BY total_spent DESC, c.name ASC;
                """;

        CreateCodingProblemRequest req = new CreateCodingProblemRequest(
                "Customer Order Summary",
                slug,
                CodingProblemType.DATABASE,
                CodingProblemDifficulty.MEDIUM,
                List.of("postgresql-16"),
                "Calculate total spent by each customer on completed orders.",
                List.of("Database", "PostgreSQL"),
                List.of(),
                Map.of("postgresql-16", "-- Write SQL query\n"),
                "postgresql-16",
                refSolution,
                false,
                null,
                null,
                List.of(new TestcaseRequest(null, initSqlSample)),
                List.of(new TestcaseRequest(null, initSqlHidden)),
                Map.of("postgresql-16", new ExecutionConstraintRequest(6000L, 262144L)),
                null,
                null,
                null,
                null,
                List.of()
        );

        CodingProblem problem = codingProblemService.create(req);
        createdProblemIds.add(problem.getId());
        return problem;
    }

    // =========================================================================
    // Generic Problem Builder: Floating Point with Epsilon Tolerance Verifier
    // =========================================================================
    protected CodingProblem createFloatToleranceVerifierProblem(String slug) {
        String verifierScript = """
                import sys, math

                raw = sys.stdin.read()
                parts = raw.split("---STACKED_INPUT_END---")
                if len(parts) != 2:
                    sys.exit(1)

                in_tokens = [float(x) for x in parts[0].strip().split()]
                out_tokens = [float(x) for x in parts[1].strip().split()]

                assert len(out_tokens) == 1, f"Expected 1 float output, got {len(out_tokens)}"

                expected = math.hypot(in_tokens[2] - in_tokens[0], in_tokens[3] - in_tokens[1])
                actual = out_tokens[0]
                diff = abs(actual - expected)

                assert diff < 1e-4, f"Output {actual} differs from expected {expected} by {diff} (tolerance 1e-4)"
                """;

        String refSolution = """
                import sys, math

                data = [float(x) for x in sys.stdin.read().split()]
                if data:
                    d = math.hypot(data[2] - data[0], data[3] - data[1])
                    print(f"{d:.6f}")
                """;

        CreateCodingProblemRequest req = new CreateCodingProblemRequest(
                "Euclidean Distance Tolerance",
                slug,
                CodingProblemType.GENERIC,
                CodingProblemDifficulty.EASY,
                List.of("python-3.12", "java-21"),
                "Calculate Euclidean distance between two 2D points.",
                List.of("Math", "Geometry"),
                List.of(),
                Map.of("python-3.12", "# solution\n"),
                "python-3.12",
                refSolution,
                true,
                "python-3.12",
                verifierScript,
                List.of(
                        new TestcaseRequest("0 0 3 4", null) // distance = 5.0
                ),
                List.of(
                        new TestcaseRequest("1 1 4 5", null), // distance = 5.0
                        new TestcaseRequest("-2 -3 1 1", null) // distance = 5.0
                ),
                Map.of(
                        "python-3.12", new ExecutionConstraintRequest(2000L, 262144L),
                        "java-21", new ExecutionConstraintRequest(3000L, 262144L)
                ),
                null,
                null,
                null,
                null,
                List.of()
        );

        CodingProblem problem = codingProblemService.create(req);
        createdProblemIds.add(problem.getId());
        return problem;
    }

    // =========================================================================
    // Generic Problem Builder: Order-Agnostic Prime Factors Verifier
    // =========================================================================
    protected CodingProblem createOrderAgnosticVerifierProblem(String slug) {
        String verifierScript = """
                import sys

                raw = sys.stdin.read()
                parts = raw.split("---STACKED_INPUT_END---")
                if len(parts) != 2:
                    sys.exit(1)

                n = int(parts[0].strip())
                cand_primes = set(int(x) for x in parts[1].strip().split())

                def get_factors(num):
                    d = 2
                    factors = set()
                    while d * d <= num:
                        if num % d == 0:
                            factors.add(d)
                            while num % d == 0:
                                num //= d
                        d += 1
                    if num > 1:
                        factors.add(num)
                    return factors

                expected_primes = get_factors(n)
                assert cand_primes == expected_primes, f"Expected factors {expected_primes}, got {cand_primes}"
                """;

        String refSolution = """
                import sys

                raw = sys.stdin.read().strip()
                if raw:
                    n = int(raw)
                    d = 2
                    factors = []
                    while d * d <= n:
                        if n % d == 0:
                            factors.append(d)
                            while n % d == 0:
                                n //= d
                        d += 1
                    if n > 1:
                        factors.append(n)
                    print(" ".join(str(x) for x in factors))
                """;

        CreateCodingProblemRequest req = new CreateCodingProblemRequest(
                "Distinct Prime Factors",
                slug,
                CodingProblemType.GENERIC,
                CodingProblemDifficulty.MEDIUM,
                List.of("python-3.12", "javascript-node-20"),
                "Output all distinct prime factors of N in any order.",
                List.of("Math", "Number Theory"),
                List.of(),
                Map.of("python-3.12", "# solution\n"),
                "python-3.12",
                refSolution,
                true,
                "python-3.12",
                verifierScript,
                List.of(
                        new TestcaseRequest("60", null) // prime factors: 2, 3, 5
                ),
                List.of(
                        new TestcaseRequest("84", null), // prime factors: 2, 3, 7
                        new TestcaseRequest("97", null)  // prime factors: 97
                ),
                Map.of(
                        "python-3.12", new ExecutionConstraintRequest(2000L, 262144L),
                        "javascript-node-20", new ExecutionConstraintRequest(2000L, 262144L)
                ),
                null,
                null,
                null,
                null,
                List.of()
        );

        CodingProblem problem = codingProblemService.create(req);
        createdProblemIds.add(problem.getId());
        return problem;
    }

    // =========================================================================
    // Database Problem Builder: Employee Salary Update (SQLite 3 with SQL Verifier)
    // =========================================================================
    protected CodingProblem createSalaryUpdateSqliteVerifierProblem(String slug) {
        String initSqlSample = """
                CREATE TABLE employee (id INTEGER PRIMARY KEY, name TEXT, salary REAL, department_id INTEGER);
                INSERT INTO employee VALUES (1, 'Alice', 1000.0, 1), (2, 'Bob', 2000.0, 1), (3, 'Charlie', 1500.0, 2);
                """;

        String initSqlHidden = """
                CREATE TABLE employee (id INTEGER PRIMARY KEY, name TEXT, salary REAL, department_id INTEGER);
                INSERT INTO employee VALUES (1, 'Dave', 5000.0, 1), (2, 'Eve', 3000.0, 3);
                """;

        String refSolution = """
                UPDATE employee SET salary = salary * 1.10 WHERE department_id = 1;
                """;

        String verifierSql = """
                SELECT id, name, CAST(ROUND(salary, 2) AS TEXT) FROM employee ORDER BY id;
                """;

        CreateCodingProblemRequest req = new CreateCodingProblemRequest(
                "Give Department 1 Raise",
                slug,
                CodingProblemType.DATABASE,
                CodingProblemDifficulty.EASY,
                List.of("sqlite-3"),
                "Update employee table to give all department 1 employees a 10% raise.",
                List.of("Database", "SQL", "DML"),
                List.of(),
                Map.of("sqlite-3", "-- Write UPDATE query\n"),
                "sqlite-3",
                refSolution,
                true,
                "sqlite-3",
                verifierSql,
                List.of(new TestcaseRequest(null, initSqlSample)),
                List.of(new TestcaseRequest(null, initSqlHidden)),
                Map.of("sqlite-3", new ExecutionConstraintRequest(5000L, 262144L)),
                null,
                null,
                null,
                null,
                List.of()
        );

        CodingProblem problem = codingProblemService.create(req);
        createdProblemIds.add(problem.getId());
        return problem;
    }
}
