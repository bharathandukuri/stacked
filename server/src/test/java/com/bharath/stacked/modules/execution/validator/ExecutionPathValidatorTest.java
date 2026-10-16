package com.bharath.stacked.modules.execution.validator;

import com.bharath.stacked.modules.execution.config.ExecutionProperties;
import com.bharath.stacked.modules.execution.dto.request.BatchCodeExecutionRequest;
import com.bharath.stacked.modules.execution.dto.request.BatchExecutionItem;
import com.bharath.stacked.modules.execution.dto.request.SimpleCodeExecutionRequest;
import com.bharath.stacked.modules.execution.exception.ExecutionValidationException;
import com.bharath.stacked.modules.language.Language;
import com.bharath.stacked.modules.language.factory.impl.LanguageFactoryImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ExecutionPathValidator Unit Tests")
class ExecutionPathValidatorTest {

    private ExecutionPathValidator validator;
    private ExecutionProperties properties;
    private final LanguageFactoryImpl languageFactory = new LanguageFactoryImpl();

    @BeforeEach
    void setUp() {
        properties = new ExecutionProperties();
        validator = new ExecutionPathValidator(properties);
    }

    @ParameterizedTest
    @ValueSource(strings = {"Solution.java", "src/main.py", "pkg/sub/module/file.c", "main.sql", "a/b/c/d.txt"})
    @DisplayName("Valid relative paths pass validation")
    void validRelativePathsPass(String path) {
        assertThatCode(() -> validator.validatePath(path, "testField")).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {"/etc/passwd", "/root/run.sh", "\\Windows\\System32", "C:/test.py", "D:\\data.sql"})
    @DisplayName("Absolute paths are rejected")
    void absolutePathsAreRejected(String path) {
        assertThatThrownBy(() -> validator.validatePath(path, "testField"))
                .isInstanceOf(ExecutionValidationException.class)
                .hasMessageContaining("Absolute paths are not allowed");
    }

    @ParameterizedTest
    @ValueSource(strings = {"../secret.txt", "../../etc/passwd", "foo/../../bar", "a/b/../../../root", ".."})
    @DisplayName("Path traversal attempts are rejected")
    void pathTraversalAttemptsAreRejected(String path) {
        assertThatThrownBy(() -> validator.validatePath(path, "testField"))
                .isInstanceOf(ExecutionValidationException.class)
                .hasMessageContaining("Path traversal attempt detected");
    }

    @Test
    @DisplayName("Null byte in path is rejected")
    void nullByteInPathIsRejected() {
        assertThatThrownBy(() -> validator.validatePath("solution\0.py", "testField"))
                .isInstanceOf(ExecutionValidationException.class)
                .hasMessageContaining("invalid null byte");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    @DisplayName("Blank path is rejected")
    void blankPathIsRejected(String path) {
        assertThatThrownBy(() -> validator.validatePath(path, "testField"))
                .isInstanceOf(ExecutionValidationException.class)
                .hasMessageContaining("must not be null or blank");
    }

    @Test
    @DisplayName("Simple request passes when valid")
    void simpleRequestValidPasses() {
        Language java = languageFactory.create("java-21");
        SimpleCodeExecutionRequest request = SimpleCodeExecutionRequest.builder()
                .language(java)
                .fileName("Solution.java")
                .code("class Solution {}")
                .build();

        assertThatCode(() -> validator.validateSimpleRequest(request)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Simple request rejects code exceeding maximum file size")
    void simpleRequestRejectsOversizedCode() {
        Language java = languageFactory.create("java-21");
        properties.getHardLimits().setMaxFileSizeBytes(10L); // 10 bytes limit
        SimpleCodeExecutionRequest request = SimpleCodeExecutionRequest.builder()
                .language(java)
                .fileName("Solution.java")
                .code("this is way more than 10 bytes of code")
                .build();

        assertThatThrownBy(() -> validator.validateSimpleRequest(request))
                .isInstanceOf(ExecutionValidationException.class)
                .hasMessageContaining("exceeds maximum allowed file size");
    }

    @Test
    @DisplayName("Batch request rejects when file count exceeds limit")
    void batchRequestRejectsExcessiveFileCount() {
        Language python = languageFactory.create("python-3.12");
        properties.getHardLimits().setMaxFileCount(2);

        Map<String, String> files = Map.of(
                "f1.py", "print(1)",
                "f2.py", "print(2)",
                "f3.py", "print(3)"
        );

        BatchCodeExecutionRequest request = BatchCodeExecutionRequest.builder()
                .language(python)
                .files(files)
                .executions(List.of(new BatchExecutionItem("f1.py", "", null)))
                .build();

        assertThatThrownBy(() -> validator.validateBatchRequest(request))
                .isInstanceOf(ExecutionValidationException.class)
                .hasMessageContaining("exceeds maximum allowed file count");
    }

    @Test
    @DisplayName("Batch request rejects when batch size exceeds limit")
    void batchRequestRejectsExcessiveBatchSize() {
        Language python = languageFactory.create("python-3.12");
        properties.getHardLimits().setMaxBatchSize(2);

        List<BatchExecutionItem> items = List.of(
                new BatchExecutionItem("f.py", "1", null),
                new BatchExecutionItem("f.py", "2", null),
                new BatchExecutionItem("f.py", "3", null)
        );

        BatchCodeExecutionRequest request = BatchCodeExecutionRequest.builder()
                .language(python)
                .files(Map.of("f.py", "print(1)"))
                .executions(items)
                .build();

        assertThatThrownBy(() -> validator.validateBatchRequest(request))
                .isInstanceOf(ExecutionValidationException.class)
                .hasMessageContaining("exceeds maximum allowed batch size");
    }

    @Test
    @DisplayName("Batch database request rejects query file not present in files map")
    void batchDatabaseRequestRejectsMissingQueryFile() {
        Language sqlite = languageFactory.create("sqlite-3");

        BatchCodeExecutionRequest request = BatchCodeExecutionRequest.builder()
                .language(sqlite)
                .files(Map.of("schema.sql", "CREATE TABLE t(id INT);"))
                .executions(List.of(new BatchExecutionItem("missing_query.sql", null, null)))
                .build();

        assertThatThrownBy(() -> validator.validateBatchRequest(request))
                .isInstanceOf(ExecutionValidationException.class)
                .hasMessageContaining("does not exist in request files");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "test;rm.py",
            "file'quote.c",
            "file\"double.cpp",
            "cmd|pipe.java",
            "bg&run.js",
            "$HOME/secret.sql",
            "`whoami`.txt",
            "sub/dir\nnewline.py",
            "path<redirect.sql"
    })
    @DisplayName("Unsafe shell and control characters in path are rejected")
    void unsafeCharactersAreRejected(String path) {
        assertThatThrownBy(() -> validator.validatePath(path, "testField"))
                .isInstanceOf(ExecutionValidationException.class)
                .hasMessageContaining("unsafe characters");
    }

    @Test
    @DisplayName("Home directory expansion in path is rejected")
    void homeExpansionIsRejected() {
        assertThatThrownBy(() -> validator.validatePath("~/code.py", "testField"))
                .isInstanceOf(ExecutionValidationException.class)
                .hasMessageContaining("Home directory expansion is not allowed");
    }

    @Test
    @DisplayName("Batch request allows executablePath matching compiled artifact name")
    void batchCompiledAllowsArtifactName() {
        Language cpp = languageFactory.create("cpp-23");

        BatchCodeExecutionRequest request = BatchCodeExecutionRequest.builder()
                .language(cpp)
                .files(Map.of("solution.cpp", "int main() {}"))
                .executions(List.of(new BatchExecutionItem("a.out", "1", null)))
                .build();

        assertThatCode(() -> validator.validateBatchRequest(request)).doesNotThrowAnyException();
    }
}

