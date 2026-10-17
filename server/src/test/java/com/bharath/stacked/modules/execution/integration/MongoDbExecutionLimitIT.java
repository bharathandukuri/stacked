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
@DisplayName("MongoDB 8.0 Comprehensive Execution Limit & Edge Case Tests")
public class MongoDbExecutionLimitIT extends BaseExecutionIT {

    @Test
    @DisplayName("Normal Execution: Insert and find document via mongosh")
    void testMongoDbNormalExecution() {
        Language mongo = languageRegistry.get("mongodb-8.0");
        assumeLanguageImageAvailable(mongo);

        String script = """
                db.users.insertOne({ name: "Alice", score: 100 });
                const user = db.users.findOne({ name: "Alice" });
                print("USER_SCORE=" + user.score);
                """;

        SimpleCodeExecutionResult result = codeExecutionService.run(SimpleCodeExecutionRequest.builder()
                .language(mongo)
                .code(script)
                .fileName("solution.js")
                .constraints(new CodeExecutionConstraints(10000L))
                .build());

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.SUCCESS);
        assertThat(result.stdout()).contains("USER_SCORE=100");
    }

    @Test
    @DisplayName("Aggregation Pipeline: Groups and computes sum in mongosh")
    void testMongoDbAggregationPipeline() {
        Language mongo = languageRegistry.get("mongodb-8.0");
        assumeLanguageImageAvailable(mongo);

        String script = """
                db.sales.insertMany([
                    { dept: "A", amount: 15 },
                    { dept: "A", amount: 25 },
                    { dept: "B", amount: 40 }
                ]);
                const agg = db.sales.aggregate([
                    { $group: { _id: "$dept", total: { $sum: "$amount" } } },
                    { $sort: { _id: 1 } }
                ]).toArray();
                print("DEPT_A=" + agg[0].total + " DEPT_B=" + agg[1].total);
                """;

        SimpleCodeExecutionResult result = codeExecutionService.run(SimpleCodeExecutionRequest.builder()
                .language(mongo)
                .code(script)
                .fileName("solution.js")
                .constraints(new CodeExecutionConstraints(10000L))
                .build());

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.SUCCESS);
        assertThat(result.stdout()).contains("DEPT_A=40 DEPT_B=40");
    }

    @Test
    @DisplayName("Time Limit Exceeded: Long-running while loop terminates on 1000ms query timeout")
    void testMongoDbTimeLimitExceeded() {
        Language mongo = languageRegistry.get("mongodb-8.0");
        assumeLanguageImageAvailable(mongo);

        String script = """
                const start = Date.now();
                while (Date.now() - start < 10000) {}
                """;

        SimpleCodeExecutionResult result = codeExecutionService.run(SimpleCodeExecutionRequest.builder()
                .language(mongo)
                .code(script)
                .fileName("solution.js")
                .constraints(new CodeExecutionConstraints(1000L)) // 1000ms query timeout
                .build());

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.TIME_LIMIT_EXCEEDED);
    }

    @Test
    @DisplayName("Runtime Error: Syntax error returns RUNTIME_ERROR")
    void testMongoDbSyntaxError() {
        Language mongo = languageRegistry.get("mongodb-8.0");
        assumeLanguageImageAvailable(mongo);

        SimpleCodeExecutionResult result = codeExecutionService.run(SimpleCodeExecutionRequest.builder()
                .language(mongo)
                .code("db.collection.find({ invalid_syntax: )")
                .fileName("solution.js")
                .build());

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.RUNTIME_ERROR);
        assertThat(result.stderr()).contains("SyntaxError");
    }

    @Test
    @DisplayName("Batch Execution: State persists across sequential mongosh scripts in same container")
    void testMongoDbBatchStatePersistence() {
        Language mongo = languageRegistry.get("mongodb-8.0");
        assumeLanguageImageAvailable(mongo);

        String step1 = """
                db.batch_test.insertOne({ count: 1 });
                """;
        String step2 = """
                db.batch_test.updateOne({}, { $inc: { count: 9 } });
                """;
        String step3 = """
                print("FINAL_COUNT=" + db.batch_test.findOne().count);
                """;

        BatchCodeExecutionRequest request = BatchCodeExecutionRequest.builder()
                .language(mongo)
                .files(Map.of(
                        "step1.js", step1,
                        "step2.js", step2,
                        "step3.js", step3
                ))
                .constraints(new CodeExecutionConstraints(10000L))
                .executions(List.of(
                        BatchExecutionItem.builder().executablePath("step1.js").build(),
                        BatchExecutionItem.builder().executablePath("step2.js").build(),
                        BatchExecutionItem.builder().executablePath("step3.js").build()
                ))
                .build();

        BatchCodeExecutionResult result = codeExecutionService.run(request);

        assertThat(result.totalExecutions()).isEqualTo(3);
        assertThat(result.successfulExecutions()).isEqualTo(3);
        assertThat(result.results().get(2).stdout()).contains("FINAL_COUNT=10");
    }
}

