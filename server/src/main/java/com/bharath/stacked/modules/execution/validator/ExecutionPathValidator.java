package com.bharath.stacked.modules.execution.validator;

import com.bharath.stacked.modules.execution.config.ExecutionProperties;
import com.bharath.stacked.modules.execution.dto.request.BatchCodeExecutionRequest;
import com.bharath.stacked.modules.execution.dto.request.BatchExecutionItem;
import com.bharath.stacked.modules.execution.dto.request.SimpleCodeExecutionRequest;
import com.bharath.stacked.modules.execution.exception.ExecutionValidationException;
import com.bharath.stacked.modules.language.Language;
import com.bharath.stacked.modules.language.LanguageType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.regex.Pattern;

@Component
public class ExecutionPathValidator {

    private static final Pattern WINDOWS_DRIVE_PATTERN = Pattern.compile("^[a-zA-Z]:[/\\\\].*");
    private static final Pattern UNSAFE_CHARACTERS_PATTERN = Pattern.compile(".*['\"`;|&$<>\n\r\t].*");

    private final ExecutionProperties executionProperties;

    public ExecutionPathValidator(ExecutionProperties executionProperties) {
        this.executionProperties = executionProperties;
    }

    public void validatePath(String path, String fieldName) {
        if (path == null || path.isBlank()) {
            throw new ExecutionValidationException("Field [" + fieldName + "] must not be null or blank.");
        }

        if (path.contains("\0")) {
            throw new ExecutionValidationException("Path contains invalid null byte in field [" + fieldName + "]: " + path);
        }

        if (path.startsWith("~")) {
            throw new ExecutionValidationException("Home directory expansion is not allowed in field [" + fieldName + "]: " + path);
        }

        if (UNSAFE_CHARACTERS_PATTERN.matcher(path).matches()) {
            throw new ExecutionValidationException("Path contains unsafe characters in field [" + fieldName + "]: " + path);
        }

        if (path.startsWith("/") || path.startsWith("\\") || WINDOWS_DRIVE_PATTERN.matcher(path).matches()) {
            throw new ExecutionValidationException("Absolute paths are not allowed in field [" + fieldName + "]: " + path);
        }

        Path normalized;
        try {
            normalized = Paths.get(path).normalize();
        } catch (Exception e) {
            throw new ExecutionValidationException("Invalid path syntax in field [" + fieldName + "]: " + path, e);
        }

        if (normalized.isAbsolute()) {
            throw new ExecutionValidationException("Absolute paths are not allowed in field [" + fieldName + "]: " + path);
        }

        for (Path segment : normalized) {
            if ("..".equals(segment.toString())) {
                throw new ExecutionValidationException("Path traversal attempt detected in field [" + fieldName + "]: " + path);
            }
        }

        String normalizedStr = normalized.toString().replace('\\', '/');
        if (normalizedStr.startsWith("../") || normalizedStr.equals("..") || normalizedStr.contains("/../")) {
            throw new ExecutionValidationException("Path traversal attempt detected in field [" + fieldName + "]: " + path);
        }
    }

    public void validateSimpleRequest(SimpleCodeExecutionRequest request) {
        if (request == null) {
            throw new ExecutionValidationException("Execution request must not be null.");
        }

        if (request.getLanguage() == null) {
            throw new ExecutionValidationException("Language must not be null.");
        }

        if (request.getFileName() != null && !request.getFileName().isBlank()) {
            validatePath(request.getFileName(), "fileName");
        }

        if (request.getCode() != null) {
            long codeSize = request.getCode().getBytes(StandardCharsets.UTF_8).length;
            long maxFileSize = executionProperties.getHardLimits().getMaxFileSizeBytes();
            if (codeSize > maxFileSize) {
                throw new ExecutionValidationException(
                        "Code size [" + codeSize + " bytes] exceeds maximum allowed file size [" + maxFileSize + " bytes]."
                );
            }
        }
    }

    public void validateBatchRequest(BatchCodeExecutionRequest request) {
        if (request == null) {
            throw new ExecutionValidationException("Batch execution request must not be null.");
        }

        Language language = request.getLanguage();
        if (language == null) {
            throw new ExecutionValidationException("Language must not be null.");
        }

        Map<String, String> files = request.getFiles();
        if (files != null && !files.isEmpty()) {
            int maxFileCount = executionProperties.getHardLimits().getMaxFileCount();
            if (files.size() > maxFileCount) {
                throw new ExecutionValidationException(
                        "File count [" + files.size() + "] exceeds maximum allowed file count [" + maxFileCount + "]."
                );
            }

            long maxFileSize = executionProperties.getHardLimits().getMaxFileSizeBytes();
            for (Map.Entry<String, String> entry : files.entrySet()) {
                String path = entry.getKey();
                validatePath(path, "files key");

                String content = entry.getValue();
                if (content != null) {
                    long size = content.getBytes(StandardCharsets.UTF_8).length;
                    if (size > maxFileSize) {
                        throw new ExecutionValidationException(
                                "File [" + path + "] size [" + size + " bytes] exceeds maximum allowed size [" + maxFileSize + " bytes]."
                        );
                    }
                }
            }
        }

        if (request.getExecutions() != null && !request.getExecutions().isEmpty()) {
            int maxBatchSize = executionProperties.getHardLimits().getMaxBatchSize();
            if (request.getExecutions().size() > maxBatchSize) {
                throw new ExecutionValidationException(
                        "Batch execution count [" + request.getExecutions().size() + "] exceeds maximum allowed batch size [" + maxBatchSize + "]."
                );
            }

            for (int i = 0; i < request.getExecutions().size(); i++) {
                BatchExecutionItem item = request.getExecutions().get(i);
                if (item != null && item.getExecutablePath() != null && !item.getExecutablePath().isBlank()) {
                    validatePath(item.getExecutablePath(), "executions[" + i + "].executablePath");

                    boolean matchesArtifact = (language instanceof com.bharath.stacked.modules.language.CompiledLanguage cl)
                            && item.getExecutablePath().equals(cl.buildArtifactName());

                    // The executable path must exist in request.getFiles() or match the compiled artifact
                    if (files != null && !files.containsKey(item.getExecutablePath()) && !matchesArtifact) {
                        throw new ExecutionValidationException(
                                "Execution file [" + item.getExecutablePath() + "] for execution item [" + i + "] does not exist in request files."
                        );
                    }
                }
            }
        }
    }
}

