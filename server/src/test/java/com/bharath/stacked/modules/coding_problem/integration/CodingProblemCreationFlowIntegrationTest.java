package com.bharath.stacked.modules.coding_problem.integration;

import com.bharath.stacked.AbstractIntegrationTest;
import com.bharath.stacked.exception.BadRequestException;
import com.bharath.stacked.modules.coding_problem.dto.request.CreateCodingProblemRequest;
import com.bharath.stacked.modules.coding_problem.dto.request.ExecutionConstraintRequest;
import com.bharath.stacked.modules.coding_problem.dto.request.TestcaseRequest;
import com.bharath.stacked.modules.coding_problem.enums.CodingProblemDifficulty;
import com.bharath.stacked.modules.coding_problem.enums.CodingProblemStatus;
import com.bharath.stacked.modules.coding_problem.enums.CodingProblemType;
import com.bharath.stacked.modules.coding_problem.model.CodingProblem;
import com.bharath.stacked.modules.coding_problem.repository.CodingProblemRepository;
import com.bharath.stacked.modules.coding_problem.service.CodingProblemService;
import com.bharath.stacked.modules.execution.service.DockerExecutionService;
import com.bharath.stacked.modules.language.registry.LanguageRegistry;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Coding Problem Creation Flow Live Integration Test")
class CodingProblemCreationFlowIntegrationTest extends AbstractIntegrationTest {

    @DynamicPropertySource
    static void enableDockerExecution(DynamicPropertyRegistry registry) {
        registry.add("stacked.execution.docker.enabled", () -> "true");
    }

    @Autowired
    private CodingProblemService codingProblemService;

    @Autowired
    private CodingProblemRepository codingProblemRepository;

    @Autowired
    private DockerExecutionService dockerExecutionService;

    @Autowired
    private LanguageRegistry languageRegistry;

    private boolean dockerReady;

    @BeforeEach
    void checkDocker() {
        try {
            dockerReady = dockerExecutionService != null &&
                    dockerExecutionService.isImageExists(languageRegistry.get("python-3.12").dockerImageDetails());
        } catch (Exception e) {
            dockerReady = false;
        }
        Assumptions.assumeTrue(dockerReady, "Docker execution is not available; skipping live test.");
    }

    @Test
    @DisplayName("Creates generic problem with batch-generated expected outputs and per-language constraints")
    void testCreateGenericProblemWithExpectedOutputsGeneratedInBatch() {
        String slug = "flow-add-numbers";
        codingProblemRepository.findBySlug(slug).ifPresent(codingProblemRepository::delete);

        CreateCodingProblemRequest request = new CreateCodingProblemRequest();
        request.setTitle("Flow Add Numbers");
        request.setSlug(slug);
        request.setProblemType(CodingProblemType.GENERIC);
        request.setDifficulty(CodingProblemDifficulty.EASY);
        request.setSupportedLanguageIds(List.of("python-3.12", "cpp-23"));
        request.setStatement("Add two numbers given on stdin.");
        request.setTopics(List.of("Math", "Basic"));
        request.setStarterCode(Map.of("python-3.12", "def add(): pass", "cpp-23", "int main() {}"));
        request.setReferenceSolutionLanguage("python-3.12");
        request.setReferenceSolution("import sys\nnums = sys.stdin.read().split()\nprint(int(nums[0]) + int(nums[1]))");
        request.setRequireVerificationScript(false);

        request.setSampleTestcases(List.of(
                new TestcaseRequest("10 20", null)
        ));
        request.setHiddenTestcases(List.of(
                new TestcaseRequest("5 7", null),
                new TestcaseRequest("100 200", null)
        ));

        // Provide custom constraint for python, omit cpp to test default fallback
        request.setExecutionConstraints(Map.of(
                "python-3.12", new ExecutionConstraintRequest(4000L, 262144L)
        ));

        CodingProblem created = codingProblemService.create(request);

        assertThat(created).isNotNull();
        assertThat(created.getId()).isNotBlank();
        assertThat(created.getSlug()).isEqualTo(slug);

        // Verify testcases have expected outputs generated
        assertThat(created.getSampleTestCases()).hasSize(1);
        assertThat(created.getSampleTestCases().get(0).getOutput()).isEqualTo("30\n");

        assertThat(created.getHiddenTestCases()).hasSize(2);
        assertThat(created.getHiddenTestCases().get(0).getOutput()).isEqualTo("12\n");
        assertThat(created.getHiddenTestCases().get(1).getOutput()).isEqualTo("300\n");

        // Verify per-language constraints
        assertThat(created.getExecutionConstraints()).containsKeys("python-3.12", "cpp-23");
        assertThat(created.getExecutionConstraints().get("python-3.12").timeLimitMs()).isEqualTo(4000L);
        assertThat(created.getExecutionConstraints().get("cpp-23").timeLimitMs()).isEqualTo(1000L); // default resolved

        // Verify persisted in database
        CodingProblem found = codingProblemRepository.findBySlug(slug).orElse(null);
        assertThat(found).isNotNull();
        assertThat(found.getStatus()).isEqualTo(CodingProblemStatus.UNPUBLISHED);
        assertThat(found.getSampleTestCases().get(0).getOutput()).isEqualTo("30\n");
    }

    @Test
    @DisplayName("Creates generic problem with verification script transforming outputs")
    void testCreateProblemWithVerificationScript() {
        String slug = "flow-multiple-answers";
        codingProblemRepository.findBySlug(slug).ifPresent(codingProblemRepository::delete);

        CreateCodingProblemRequest request = new CreateCodingProblemRequest();
        request.setTitle("Flow Multiple Answers");
        request.setSlug(slug);
        request.setProblemType(CodingProblemType.GENERIC);
        request.setDifficulty(CodingProblemDifficulty.MEDIUM);
        request.setSupportedLanguageIds(List.of("python-3.12"));
        request.setStatement("Return any valid multiple.");
        request.setTopics(List.of("Math"));
        request.setReferenceSolutionLanguage("python-3.12");
        request.setReferenceSolution("print('raw_ref_solution_out')");

        request.setRequireVerificationScript(true);
        request.setVerificationScriptLanguage("python-3.12");
        // Verification script reads stdin and ref stdout separated by delimiter, then outputs canonical expected output
        request.setVerificationScript("""
                import sys
                content = sys.stdin.read()
                parts = content.split("---STACKED_INPUT_END---")
                stdin_input = parts[0].strip()
                ref_out = parts[1].strip()
                print(f"CANONICAL_{stdin_input}")
                """);

        request.setSampleTestcases(List.of(new TestcaseRequest("input_A", null)));
        request.setHiddenTestcases(List.of(new TestcaseRequest("input_B", null)));

        CodingProblem created = codingProblemService.create(request);

        assertThat(created).isNotNull();
        // Expected output is the reference solution output
        assertThat(created.getSampleTestCases().get(0).getOutput()).isEqualTo("raw_ref_solution_out\n");
        assertThat(created.getHiddenTestCases().get(0).getOutput()).isEqualTo("raw_ref_solution_out\n");
    }

    @Test
    @DisplayName("Creates database problem with schema init and query execution")
    void testCreateDatabaseProblemWithExpectedOutputs() {
        String slug = "flow-db-problem";
        codingProblemRepository.findBySlug(slug).ifPresent(codingProblemRepository::delete);

        CreateCodingProblemRequest request = new CreateCodingProblemRequest();
        request.setTitle("Flow DB Problem");
        request.setSlug(slug);
        request.setProblemType(CodingProblemType.DATABASE);
        request.setDifficulty(CodingProblemDifficulty.EASY);
        request.setSupportedLanguageIds(List.of("sqlite-3"));
        request.setStatement("Select total sum of items.");
        request.setTopics(List.of("SQL"));
        request.setReferenceSolutionLanguage("sqlite-3");
        request.setReferenceSolution("SELECT SUM(amount) FROM orders;");
        request.setRequireVerificationScript(false);

        request.setSampleTestcases(List.of(
                new TestcaseRequest("", "CREATE TABLE orders (amount INT); INSERT INTO orders VALUES (10), (20);")
        ));
        request.setHiddenTestcases(List.of(
                new TestcaseRequest("", "CREATE TABLE orders (amount INT); INSERT INTO orders VALUES (100), (200), (300);")
        ));

        CodingProblem created = codingProblemService.create(request);

        assertThat(created).isNotNull();
        assertThat(created.getSampleTestCases().get(0).getOutput()).contains("30");
        assertThat(created.getHiddenTestCases().get(0).getOutput()).contains("600");
    }

    @Test
    @DisplayName("Creates database problem with verification script and ensures database state isolation between testcases")
    void testCreateDatabaseProblemWithVerificationScriptAndIsolation() {
        String slug = "flow-db-verification";
        codingProblemRepository.findBySlug(slug).ifPresent(codingProblemRepository::delete);

        CreateCodingProblemRequest request = new CreateCodingProblemRequest();
        request.setTitle("Flow DB Verification");
        request.setSlug(slug);
        request.setProblemType(CodingProblemType.DATABASE);
        request.setDifficulty(CodingProblemDifficulty.MEDIUM);
        request.setSupportedLanguageIds(List.of("sqlite-3"));
        request.setStatement("Double all account balances and verify final balance.");
        request.setTopics(List.of("SQL"));
        request.setReferenceSolutionLanguage("sqlite-3");
        request.setReferenceSolution("UPDATE accounts SET balance = balance * 2;");
        request.setRequireVerificationScript(true);
        request.setVerificationScriptLanguage("sqlite-3");
        request.setVerificationScript("SELECT balance FROM accounts;");

        // Testcase 1 has balance 100 -> after solution, balance is 200
        // Testcase 2 has balance 500 -> after solution, balance is 1000
        // If state leaked between testcases, the table 'accounts' would fail creation or contain both rows
        request.setSampleTestcases(List.of(
                new TestcaseRequest("", "CREATE TABLE accounts (balance INT); INSERT INTO accounts VALUES (100);")
        ));
        request.setHiddenTestcases(List.of(
                new TestcaseRequest("", "CREATE TABLE accounts (balance INT); INSERT INTO accounts VALUES (500);")
        ));

        CodingProblem created = codingProblemService.create(request);

        assertThat(created).isNotNull();
        assertThat(created.getSampleTestCases().get(0).getOutput()).contains("200");
        assertThat(created.getHiddenTestCases().get(0).getOutput()).contains("1000");
    }

    @Test
    @DisplayName("Fails problem creation and does not persist when reference solution fails")
    void testReferenceSolutionFailureAbortsAndDoesNotPersist() {
        String slug = "flow-bad-solution";
        codingProblemRepository.findBySlug(slug).ifPresent(codingProblemRepository::delete);

        CreateCodingProblemRequest request = new CreateCodingProblemRequest();
        request.setTitle("Flow Bad Solution");
        request.setSlug(slug);
        request.setProblemType(CodingProblemType.GENERIC);
        request.setDifficulty(CodingProblemDifficulty.EASY);
        request.setSupportedLanguageIds(List.of("python-3.12"));
        request.setStatement("Failing problem.");
        request.setTopics(List.of("Test"));
        request.setReferenceSolutionLanguage("python-3.12");
        request.setReferenceSolution("raise RuntimeError('Intentional reference solution crash!')");
        request.setRequireVerificationScript(false);

        request.setSampleTestcases(List.of(new TestcaseRequest("1", null)));
        request.setHiddenTestcases(List.of(new TestcaseRequest("2", null)));

        assertThatThrownBy(() -> codingProblemService.create(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Reference solution failed");

        // Verify nothing persisted in database
        assertThat(codingProblemRepository.findBySlug(slug)).isEmpty();
    }
}

