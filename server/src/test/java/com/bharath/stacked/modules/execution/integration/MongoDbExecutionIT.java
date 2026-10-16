package com.bharath.stacked.modules.execution.integration;

import com.bharath.stacked.modules.execution.dto.CodeExecutionConstraints;
import com.bharath.stacked.modules.execution.dto.request.SimpleCodeExecutionRequest;
import com.bharath.stacked.modules.execution.dto.response.SimpleCodeExecutionResult;
import com.bharath.stacked.modules.execution.enums.CodeExecutionStatus;
import com.bharath.stacked.modules.language.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MongoDB 8.0 Live Integration Tests")
class MongoDbExecutionIT extends BaseExecutionIT {

    @Test
    @DisplayName("Waits for mongod readiness and executes mongosh script via --file")
    void executeMongoDbSuccess() {
        Language mongo = languageRegistry.get("mongodb-8.0");
        assumeLanguageImageAvailable(mongo);

        String script = """
                db.users.insertOne({ name: "Alice", score: 100 });
                const user = db.users.findOne({ name: "Alice" });
                print("USER_SCORE=" + user.score);
                """;

        SimpleCodeExecutionRequest request = SimpleCodeExecutionRequest.builder()
                .language(mongo)
                .code(script)
                .fileName("solution.js")
                .constraints(new CodeExecutionConstraints(10000L))
                .build();

        SimpleCodeExecutionResult result = codeExecutionService.run(request);

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.SUCCESS);
        assertThat(result.stdout()).contains("USER_SCORE=100");
    }

    @Test
    @DisplayName("MongoDB script timeout terminates sleep loop")
    void executeMongoDbTimeout() {
        Language mongo = languageRegistry.get("mongodb-8.0");
        assumeLanguageImageAvailable(mongo);

        String script = "sleep(15000);";

        SimpleCodeExecutionRequest request = SimpleCodeExecutionRequest.builder()
                .language(mongo)
                .code(script)
                .fileName("solution.js")
                .constraints(new CodeExecutionConstraints(1000L))
                .build();

        SimpleCodeExecutionResult result = codeExecutionService.run(request);

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.TIME_LIMIT_EXCEEDED);
    }
}

