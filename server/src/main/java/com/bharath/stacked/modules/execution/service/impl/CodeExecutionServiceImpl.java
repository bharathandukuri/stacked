    package com.bharath.stacked.modules.execution.service.impl;
    
    import com.bharath.stacked.modules.execution.config.ExecutionProperties;
    import com.bharath.stacked.modules.execution.dto.*;
    import com.bharath.stacked.modules.execution.dto.request.BatchCodeExecutionRequest;
    import com.bharath.stacked.modules.execution.dto.request.BatchExecutionItem;
    import com.bharath.stacked.modules.execution.dto.request.SimpleCodeExecutionRequest;
    import com.bharath.stacked.modules.execution.dto.response.BatchCodeExecutionResult;
    import com.bharath.stacked.modules.execution.dto.response.DockerExecutionResult;
    import com.bharath.stacked.modules.execution.dto.response.IsolateExecutionResult;
    import com.bharath.stacked.modules.execution.dto.response.SimpleCodeExecutionResult;
    import com.bharath.stacked.modules.execution.enums.CodeExecutionStatus;
    import com.bharath.stacked.modules.execution.exception.DockerExecutionException;
    import com.bharath.stacked.modules.execution.exception.ExecutionValidationException;
    import com.bharath.stacked.modules.execution.mapper.CodeExecutionStatusMapper;
    import com.bharath.stacked.modules.execution.resolver.ExecutionConstraintResolver;
    import com.bharath.stacked.modules.execution.service.CodeExecutionService;
    import com.bharath.stacked.modules.execution.service.DockerExecutionService;
    import com.bharath.stacked.modules.execution.service.IsolateExecutionService;
    import com.bharath.stacked.modules.execution.validator.ExecutionPathValidator;
    import com.bharath.stacked.modules.language.CompiledLanguage;
    import com.bharath.stacked.modules.language.DatabaseLanguage;
    import com.bharath.stacked.modules.language.Language;
    import com.bharath.stacked.modules.language.LanguageType;
    import lombok.extern.slf4j.Slf4j;
    import org.jspecify.annotations.NonNull;
    import org.springframework.beans.factory.annotation.Autowired;
    import org.springframework.stereotype.Service;
    
    import java.util.ArrayList;
    import java.util.List;
    import java.util.Map;
    
    @Slf4j
    @Service
    public class CodeExecutionServiceImpl implements CodeExecutionService {
    
        private final DockerExecutionService dockerExecutionService;
        private final IsolateExecutionService isolateExecutionService;
        private final CodeExecutionStatusMapper codeExecutionStatusMapper;
        private final ExecutionProperties executionProperties;
        private final ExecutionPathValidator executionPathValidator;
        private final ExecutionConstraintResolver executionConstraintResolver;
        private final com.bharath.stacked.modules.execution.queue.ExecutionQueueService executionQueueService;
    
        @Autowired
        public CodeExecutionServiceImpl(
                DockerExecutionService dockerExecutionService,
                IsolateExecutionService isolateExecutionService,
                CodeExecutionStatusMapper codeExecutionStatusMapper,
                ExecutionProperties executionProperties,
                ExecutionPathValidator executionPathValidator,
                ExecutionConstraintResolver executionConstraintResolver,
                @Autowired(required = false) com.bharath.stacked.modules.execution.queue.ExecutionQueueService executionQueueService) {
            this.dockerExecutionService = dockerExecutionService;
            this.isolateExecutionService = isolateExecutionService;
            this.codeExecutionStatusMapper = codeExecutionStatusMapper;
            this.executionProperties = executionProperties;
            this.executionPathValidator = executionPathValidator;
            this.executionConstraintResolver = executionConstraintResolver;
            this.executionQueueService = executionQueueService;
        }
    
        /**
         * Backward-compatible constructor for testing and manual wiring.
         */
        public CodeExecutionServiceImpl(
                DockerExecutionService dockerExecutionService,
                IsolateExecutionService isolateExecutionService,
                CodeExecutionStatusMapper codeExecutionStatusMapper,
                ExecutionProperties executionProperties,
                ExecutionPathValidator executionPathValidator,
                ExecutionConstraintResolver executionConstraintResolver) {
            this(
                    dockerExecutionService,
                    isolateExecutionService,
                    codeExecutionStatusMapper,
                    executionProperties,
                    executionPathValidator,
                    executionConstraintResolver,
                    null
            );
        }
    
        /**
         * Backward-compatible constructor for testing and manual wiring.
         */
        public CodeExecutionServiceImpl(
                DockerExecutionService dockerExecutionService,
                IsolateExecutionService isolateExecutionService,
                CodeExecutionStatusMapper codeExecutionStatusMapper) {
            this(
                    dockerExecutionService,
                    isolateExecutionService,
                    codeExecutionStatusMapper,
                    new ExecutionProperties(),
                    new ExecutionPathValidator(new ExecutionProperties()),
                    new ExecutionConstraintResolver(new ExecutionProperties()),
                    null
            );
        }
    
        @Override
        public SimpleCodeExecutionResult run(SimpleCodeExecutionRequest request) {
            if (request == null || request.getLanguage() == null) {
                return SimpleCodeExecutionResult.builder()
                        .executionStatus(CodeExecutionStatus.SYSTEM_ERROR)
                        .logs(List.of("Execution request and language must not be null."))
                        .build();
            }
    
            // 1. Validate paths and input limits BEFORE resource allocation
            try {
                executionPathValidator.validateSimpleRequest(request);
            } catch (ExecutionValidationException e) {
                log.warn("Path validation failed for execution request: {}", e.getMessage());
                return SimpleCodeExecutionResult.builder()
                        .executionStatus(CodeExecutionStatus.SYSTEM_ERROR)
                        .logs(List.of("Validation failed: " + e.getMessage()))
                        .build();
            }
    
            if (executionQueueService != null) {
                return executionQueueService.execute(resolveCurrentUserId(), () -> doRunSimple(request));
            }
            return doRunSimple(request);
        }
    
        private SimpleCodeExecutionResult doRunSimple(SimpleCodeExecutionRequest request) {
            Language language = request.getLanguage();
            DockerImageDetails dockerImage = language.dockerImageDetails();
    
            if (language.type() == LanguageType.DATABASE) {
                return executeDatabase(request, language, dockerImage);
            }
    
            DockerContainerDetails dockerContainer = null;
            IsolateSandBoxDetails sandBoxDetails = null;
    
            try {
                dockerContainer = dockerExecutionService.createContainer(dockerImage);
                dockerExecutionService.startContainer(dockerContainer.id());
    
                sandBoxDetails = isolateExecutionService.initialize(dockerContainer);
                String boxDir = "/var/lib/isolate/" + sandBoxDetails.isolateBoxId() + "/box";
    
                String fileName = (request.getFileName() != null && !request.getFileName().isBlank())
                        ? request.getFileName()
                        : language.defaultFileName();
                String code = request.getCode() != null ? request.getCode() : "";
    
                dockerExecutionService.writeFile(
                        dockerContainer.id(),
                        boxDir + "/" + fileName,
                        code);
    
                if (language.type() == LanguageType.COMPILED) {
                    List<String> bashCompileCmd = getBashCompileCmd((CompiledLanguage) language, fileName, boxDir);
                    long compilationTimeoutMs = executionConstraintResolver.resolveCompilationTimeoutMs(language);
                    DockerExecutionResult compileResult = dockerExecutionService.execContainer(
                            dockerContainer.id(),
                            bashCompileCmd,
                            compilationTimeoutMs);
    
                    if (compileResult.exitCode() != 0) {
                        log.warn(
                                "Compilation failed for language [{}] with exit code {}: {}",
                                language.id(),
                                compileResult.exitCode(),
                                compileResult.stderr());
    
                        return SimpleCodeExecutionResult.builder()
                                .stdout(compileResult.stdout())
                                .stderr(compileResult.stderr())
                                .exitCode(compileResult.exitCode())
                                .executionStatus(CodeExecutionStatus.COMPILATION_ERROR)
                                .logs(List.of("Compilation failed with exit code: " + compileResult.exitCode()))
                                .build();
                    }
                }
    
                CodeExecutionConstraints effectiveConstraints = new CodeExecutionConstraints(
                        executionConstraintResolver.resolveTimeLimitMs(language, request.getConstraints(), null),
                        executionConstraintResolver.resolveMemoryLimitKb(language, request.getConstraints(), null)
                );
                IsolateExecutionConstraints isolateConstraints =
                        executionConstraintResolver.resolveIsolateConstraints(language, request.getConstraints(), null);

                List<String> runCommand = wrapWithBash(language.run(fileName, effectiveConstraints));
    
                IsolateExecutionResult isolateResult = isolateExecutionService.executeWithConstraints(
                        sandBoxDetails,
                        runCommand,
                        request.getStdin() != null ? request.getStdin() : "",
                        isolateConstraints);
    
                Long execTimeMs = null;
                if (isolateResult.wallTimeSeconds() != null) {
                    execTimeMs = (long) (isolateResult.wallTimeSeconds() * 1000);
                } else if (isolateResult.cpuTimeSeconds() != null) {
                    execTimeMs = (long) (isolateResult.cpuTimeSeconds() * 1000);
                }
    
                List<String> logs = buildExecutionLogs(isolateResult);
    
                return SimpleCodeExecutionResult.builder()
                        .stdout(isolateResult.stdout())
                        .stderr(isolateResult.stderr())
                        .exitCode(isolateResult.exitCode())
                        .exitSignal(isolateResult.exitSignal())
                        .executionStatus(codeExecutionStatusMapper.map(isolateResult.status()))
                        .executionTimeMs(execTimeMs)
                        .memoryUsageKb(isolateResult.memoryKb())
                        .logs(logs)
                        .build();
    
            } catch (Exception e) {
                log.error("Failed to run code execution.", e);
    
                return SimpleCodeExecutionResult.builder()
                        .executionStatus(CodeExecutionStatus.SYSTEM_ERROR)
                        .logs(List.of(e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()))
                        .build();
    
            } finally {
                if (sandBoxDetails != null) {
                    try {
                        isolateExecutionService.cleanup(sandBoxDetails);
                    } catch (Exception e) {
                        log.warn("Failed to cleanup Isolate sandbox [boxId={}]: {}", sandBoxDetails.isolateBoxId(),
                                e.getMessage());
                    }
                }
    
                cleanup(dockerContainer);
            }
        }
    
        @Override
        public BatchCodeExecutionResult run(BatchCodeExecutionRequest request) {
            if (request == null || request.getLanguage() == null) {
                return BatchCodeExecutionResult.builder()
                        .results(List.of())
                        .totalExecutionTimeMs(0L)
                        .totalExecutions(0)
                        .successfulExecutions(0)
                        .failedExecutions(0)
                        .build();
            }
    
            if (request.getExecutions() == null || request.getExecutions().isEmpty()) {
                return BatchCodeExecutionResult.builder()
                        .results(List.of())
                        .totalExecutionTimeMs(0L)
                        .totalExecutions(0)
                        .successfulExecutions(0)
                        .failedExecutions(0)
                        .build();
            }
    
            // 1. Validate paths and input limits BEFORE resource allocation
            try {
                executionPathValidator.validateBatchRequest(request);
            } catch (ExecutionValidationException e) {
                log.warn("Path validation failed for batch execution request: {}", e.getMessage());
                int total = request.getExecutions().size();
                List<SimpleCodeExecutionResult> results = new ArrayList<>(total);
                for (int i = 0; i < total; i++) {
                    results.add(SimpleCodeExecutionResult.builder()
                            .executionStatus(CodeExecutionStatus.SYSTEM_ERROR)
                            .logs(List.of("Validation failed: " + e.getMessage()))
                            .build());
                }
                return BatchCodeExecutionResult.builder()
                        .results(results)
                        .totalExecutionTimeMs(0L)
                        .totalExecutions(total)
                        .successfulExecutions(0)
                        .failedExecutions(total)
                        .build();
            }
    
            if (executionQueueService != null) {
                return executionQueueService.execute(resolveCurrentUserId(), () -> doRunBatch(request));
            }
            return doRunBatch(request);
        }
    
        private BatchCodeExecutionResult doRunBatch(BatchCodeExecutionRequest request) {
            Language language = request.getLanguage();
            DockerImageDetails dockerImage = language.dockerImageDetails();
    
            if (language.type() == LanguageType.DATABASE) {
                return executeDatabaseBatch(request, language, dockerImage);
            }
    
            DockerContainerDetails dockerContainer = null;
            IsolateSandBoxDetails sandBoxDetails = null;
    
            try {
                dockerContainer = dockerExecutionService.createContainer(dockerImage);
                dockerExecutionService.startContainer(dockerContainer.id());
    
                sandBoxDetails = isolateExecutionService.initialize(dockerContainer);
                String boxDir = "/var/lib/isolate/" + sandBoxDetails.isolateBoxId() + "/box";
    
                // Write all batch files to the sandbox box directory
                if (request.getFiles() != null && !request.getFiles().isEmpty()) {
                    for (Map.Entry<String, String> entry : request.getFiles().entrySet()) {
                        String relativePath = entry.getKey().replace('\\', '/');
                        String content = entry.getValue() != null ? entry.getValue() : "";
                        dockerExecutionService.writeFile(
                                dockerContainer.id(),
                                boxDir + "/" + relativePath,
                                content
                        );
                    }
                }
    
                // Compilation step (once per batch for COMPILED languages)
                if (language.type() == LanguageType.COMPILED) {
                    CompiledLanguage compiledLang = (CompiledLanguage) language;
                    List<String> filesToCompile = compiledLang.resolveFilesToCompile(request.getFiles());
    
                    List<String> bashCompileCmd = getBashCompileCmd(compiledLang, filesToCompile, boxDir);
                    long compilationTimeoutMs = executionConstraintResolver.resolveCompilationTimeoutMs(language);
                    DockerExecutionResult compileResult = dockerExecutionService.execContainer(
                            dockerContainer.id(),
                            bashCompileCmd,
                            compilationTimeoutMs
                    );
    
                    if (compileResult.exitCode() != 0) {
                        log.warn(
                                "Batch compilation failed for language [{}] with exit code {}: {}",
                                language.id(),
                                compileResult.exitCode(),
                                compileResult.stderr()
                        );
    
                        SimpleCodeExecutionResult compError = SimpleCodeExecutionResult.builder()
                                .stdout(compileResult.stdout())
                                .stderr(compileResult.stderr())
                                .exitCode(compileResult.exitCode())
                                .executionStatus(CodeExecutionStatus.COMPILATION_ERROR)
                                .logs(List.of("Compilation failed with exit code: " + compileResult.exitCode()))
                                .build();
    
                        int total = request.getExecutions().size();
                        List<SimpleCodeExecutionResult> results = new ArrayList<>(total);
                        for (int i = 0; i < total; i++) {
                            results.add(compError);
                        }
    
                        return BatchCodeExecutionResult.builder()
                                .results(results)
                                .totalExecutionTimeMs(0L)
                                .totalExecutions(total)
                                .successfulExecutions(0)
                                .failedExecutions(total)
                                .build();
                    }
                }
    
                // Execution of individual testcases / batch execution items
                List<SimpleCodeExecutionResult> results = new ArrayList<>();
                long totalTimeMs = 0L;
                int successCount = 0;
                int failedCount = 0;
    
                String defaultExecutable = language.resolveMainExecutable(request.getFiles());
    
                for (int i = 0; i < request.getExecutions().size(); i++) {
                    BatchExecutionItem item = request.getExecutions().get(i);
                    try {
                        CodeExecutionConstraints effectiveConstraints = new CodeExecutionConstraints(
                                executionConstraintResolver.resolveTimeLimitMs(language, item.getConstraints(), request.getConstraints()),
                                executionConstraintResolver.resolveMemoryLimitKb(language, item.getConstraints(), request.getConstraints())
                        );
                        IsolateExecutionConstraints isolateConstraints =
                                executionConstraintResolver.resolveIsolateConstraints(
                                        language,
                                        item.getConstraints(),
                                        request.getConstraints()
                                );

                        String targetExecutable = (item.getExecutablePath() != null && !item.getExecutablePath().isBlank())
                                ? item.getExecutablePath()
                                : defaultExecutable;

                        List<String> runCommand = wrapWithBash(language.run(targetExecutable, effectiveConstraints));
                        String stdin = item.getStdin() != null ? item.getStdin() : "";
    
                        IsolateExecutionResult isolateResult = isolateExecutionService.executeWithConstraints(
                                sandBoxDetails,
                                runCommand,
                                stdin,
                                isolateConstraints
                        );
    
                        Long execTimeMs = null;
                        if (isolateResult.wallTimeSeconds() != null) {
                            execTimeMs = (long) (isolateResult.wallTimeSeconds() * 1000);
                        } else if (isolateResult.cpuTimeSeconds() != null) {
                            execTimeMs = (long) (isolateResult.cpuTimeSeconds() * 1000);
                        }
    
                        if (execTimeMs != null) {
                            totalTimeMs += execTimeMs;
                        }
    
                        CodeExecutionStatus status = codeExecutionStatusMapper.map(isolateResult.status());
                        if (status == CodeExecutionStatus.SUCCESS) {
                            successCount++;
                        } else {
                            failedCount++;
                        }
    
                        List<String> logs = buildExecutionLogs(isolateResult);
    
                        results.add(SimpleCodeExecutionResult.builder()
                                .stdout(isolateResult.stdout())
                                .stderr(isolateResult.stderr())
                                .exitCode(isolateResult.exitCode())
                                .exitSignal(isolateResult.exitSignal())
                                .executionStatus(status)
                                .executionTimeMs(execTimeMs)
                                .memoryUsageKb(isolateResult.memoryKb())
                                .logs(logs)
                                .build());
    
                    } catch (Exception e) {
                        log.error("Execution item at index [{}] failed.", i, e);
                        failedCount++;
                        results.add(SimpleCodeExecutionResult.builder()
                                .executionStatus(CodeExecutionStatus.SYSTEM_ERROR)
                                .logs(List.of("Execution failed: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName())))
                                .build());
                    }
                }
    
                return BatchCodeExecutionResult.builder()
                        .results(results)
                        .totalExecutionTimeMs(totalTimeMs)
                        .totalExecutions(results.size())
                        .successfulExecutions(successCount)
                        .failedExecutions(failedCount)
                        .build();
    
            } catch (Exception e) {
                log.error("Failed to run batch code execution.", e);
                int count = request.getExecutions() != null ? request.getExecutions().size() : 0;
                List<SimpleCodeExecutionResult> results = new ArrayList<>(count);
                for (int i = 0; i < count; i++) {
                    results.add(SimpleCodeExecutionResult.builder()
                            .executionStatus(CodeExecutionStatus.SYSTEM_ERROR)
                            .logs(List.of(e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()))
                            .build());
                }
                return BatchCodeExecutionResult.builder()
                        .results(results)
                        .totalExecutionTimeMs(0L)
                        .totalExecutions(count)
                        .successfulExecutions(0)
                        .failedExecutions(count)
                        .build();
    
            } finally {
                if (sandBoxDetails != null) {
                    try {
                        isolateExecutionService.cleanup(sandBoxDetails);
                    } catch (Exception e) {
                        log.warn("Failed to cleanup Isolate sandbox [boxId={}]: {}", sandBoxDetails.isolateBoxId(),
                                e.getMessage());
                    }
                }
    
                cleanup(dockerContainer);
            }
        }
    
        private void cleanup(DockerContainerDetails dockerContainer) {
            if (dockerContainer != null) {
                try {
                    dockerExecutionService.stopContainer(dockerContainer.id());
                } catch (Exception e) {
                    log.warn("Failed to stop Docker container [{}]: {}", dockerContainer.id(), e.getMessage());
                }
    
                try {
                    dockerExecutionService.deleteContainer(dockerContainer.id());
                } catch (Exception e) {
                    log.warn("Failed to delete Docker container [{}]: {}", dockerContainer.id(), e.getMessage());
                }
            }
        }
    
        private SimpleCodeExecutionResult executeDatabase(
                SimpleCodeExecutionRequest request,
                Language language,
                DockerImageDetails dockerImage) {
            DockerContainerDetails dockerContainer = null;
            try {
                DockerContainerConstraints constraints =
                        executionConstraintResolver.resolveDatabaseContainerConstraints(language);
                dockerContainer = dockerExecutionService.createContainer(dockerImage, constraints);
                dockerExecutionService.startContainer(dockerContainer.id());
                waitForDatabaseReady(dockerContainer.id(), language);
    
                String fileName = (request.getFileName() != null && !request.getFileName().isBlank())
                        ? request.getFileName()
                        : language.defaultFileName();
                String code = request.getCode() != null ? request.getCode() : "";
                String targetPath = "/tmp/" + fileName;
    
                dockerExecutionService.writeFile(
                        dockerContainer.id(),
                        targetPath,
                        code);
    
                long timeLimitMs = executionConstraintResolver.resolveDatabaseTimeoutMs(language, request.getConstraints(), null);
    
                List<String> runCommand = language.run(targetPath);
                long startNanos = System.nanoTime();
                DockerExecutionResult execResult = dockerExecutionService.execContainer(
                        dockerContainer.id(),
                        runCommand,
                        timeLimitMs);
                long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000L;
    
                String cleanStderr = sanitizeDatabaseStderr(execResult.stderr());
                CodeExecutionStatus status = getCodeExecutionStatus(execResult, cleanStderr);
    
                return SimpleCodeExecutionResult.builder()
                        .stdout(execResult.stdout())
                        .stderr(cleanStderr)
                        .exitCode(execResult.exitCode())
                        .exitSignal(0L)
                        .executionStatus(status)
                        .executionTimeMs(elapsedMs)
                        .memoryUsageKb(null)
                        .logs(List.of("Database execution completed with exit code: " + execResult.exitCode()))
                        .build();
    
            } catch (Exception e) {
                log.error("Failed to run database code execution.", e);
                return SimpleCodeExecutionResult.builder()
                        .executionStatus(CodeExecutionStatus.SYSTEM_ERROR)
                        .logs(List.of(e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()))
                        .build();
    
            } finally {
                cleanup(dockerContainer);
            }
        }
    
        private BatchCodeExecutionResult executeDatabaseBatch(
                BatchCodeExecutionRequest request,
                Language language,
                DockerImageDetails dockerImage) {
            DockerContainerDetails dockerContainer = null;
            try {
                DockerContainerConstraints constraints =
                        executionConstraintResolver.resolveDatabaseContainerConstraints(language);
                dockerContainer = dockerExecutionService.createContainer(dockerImage, constraints);
                dockerExecutionService.startContainer(dockerContainer.id());
                waitForDatabaseReady(dockerContainer.id(), language);
    
                // 1. Write all batch files to /tmp/ in the database container
                if (request.getFiles() != null && !request.getFiles().isEmpty()) {
                    for (Map.Entry<String, String> entry : request.getFiles().entrySet()) {
                        String targetPath = "/tmp/" + entry.getKey().replace('\\', '/');
                        dockerExecutionService.writeFile(
                                dockerContainer.id(),
                                targetPath,
                                entry.getValue() != null ? entry.getValue() : ""
                        );
                    }
                }
    
                List<SimpleCodeExecutionResult> results = new ArrayList<>();
                long totalTimeMs = 0L;
                int successCount = 0;
                int failedCount = 0;
    
                String defaultExecutable = language.resolveMainExecutable(request.getFiles());
    
                // 2. Execute each batch execution item referencing script files (no stdin as code)
                for (int i = 0; i < request.getExecutions().size(); i++) {
                    BatchExecutionItem item = request.getExecutions().get(i);
                    try {
                        long timeLimitMs = executionConstraintResolver.resolveDatabaseTimeoutMs(
                                language,
                                item.getConstraints(),
                                request.getConstraints()
                        );
    
                        String targetFile = (item.getExecutablePath() != null && !item.getExecutablePath().isBlank())
                                ? item.getExecutablePath()
                                : defaultExecutable;
                        String queryFilePath = "/tmp/" + targetFile;
    
                        List<String> runCommand = language.run(queryFilePath);
                        long startNanos = System.nanoTime();
                        DockerExecutionResult execResult = dockerExecutionService.execContainer(
                                dockerContainer.id(),
                                runCommand,
                                timeLimitMs
                        );
                        long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000L;
                        totalTimeMs += elapsedMs;
    
                        String cleanStderr = sanitizeDatabaseStderr(execResult.stderr());
                        CodeExecutionStatus status = getCodeExecutionStatus(execResult, cleanStderr);
                        if (status == CodeExecutionStatus.SUCCESS) {
                            successCount++;
                        } else {
                            failedCount++;
                        }
    
                        results.add(SimpleCodeExecutionResult.builder()
                                .stdout(execResult.stdout())
                                .stderr(cleanStderr)
                                .exitCode(execResult.exitCode())
                                .exitSignal(0L)
                                .executionStatus(status)
                                .executionTimeMs(elapsedMs)
                                .memoryUsageKb(null)
                                .logs(List.of("Database execution completed with exit code: " + execResult.exitCode()))
                                .build());
                    } catch (Exception e) {
                        log.error("Database batch execution item at index [{}] failed.", i, e);
                        failedCount++;
                        results.add(SimpleCodeExecutionResult.builder()
                                .executionStatus(CodeExecutionStatus.SYSTEM_ERROR)
                                .logs(List.of("Execution failed: "
                                        + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName())))
                                .build());
                    }
                }
    
                return BatchCodeExecutionResult.builder()
                        .results(results)
                        .totalExecutionTimeMs(totalTimeMs)
                        .totalExecutions(results.size())
                        .successfulExecutions(successCount)
                        .failedExecutions(failedCount)
                        .build();
    
            } catch (Exception e) {
                log.error("Failed to run database batch code execution.", e);
                int count = request.getExecutions() != null ? request.getExecutions().size() : 0;
                List<SimpleCodeExecutionResult> results = new ArrayList<>(count);
                for (int i = 0; i < count; i++) {
                    results.add(SimpleCodeExecutionResult.builder()
                            .executionStatus(CodeExecutionStatus.SYSTEM_ERROR)
                            .logs(List.of(e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()))
                            .build());
                }
                return BatchCodeExecutionResult.builder()
                        .results(results)
                        .totalExecutionTimeMs(0L)
                        .totalExecutions(count)
                        .successfulExecutions(0)
                        .failedExecutions(count)
                        .build();
    
            } finally {
                cleanup(dockerContainer);
            }
        }
    
        private static @NonNull CodeExecutionStatus getCodeExecutionStatus(DockerExecutionResult execResult, String cleanStderr) {
            CodeExecutionStatus status;
            if (execResult.exitCode() == 0L) {
                if (cleanStderr != null && !cleanStderr.isBlank()) {
                    String lower = cleanStderr.toLowerCase();
                    if (lower.contains("error:") || lower.contains("error ") || lower.contains("fatal:") || lower.contains("parse error")) {
                        status = CodeExecutionStatus.RUNTIME_ERROR;
                    } else {
                        status = CodeExecutionStatus.SUCCESS;
                    }
                } else {
                    status = CodeExecutionStatus.SUCCESS;
                }
            } else if (execResult.exitCode() == 124L) {
                status = CodeExecutionStatus.TIME_LIMIT_EXCEEDED;
            } else {
                status = CodeExecutionStatus.RUNTIME_ERROR;
            }
            return status;
        }
    
        private String sanitizeDatabaseStderr(String stderr) {
            if (stderr == null || stderr.isBlank()) {
                return stderr;
            }
            String filtered = stderr.lines()
                    .filter(line -> !line.contains("Using a password on the command line interface can be insecure"))
                    .reduce((a, b) -> a + "\n" + b)
                    .orElse("")
                    .trim();
            return filtered.isEmpty() ? null : filtered;
        }
    
        private void waitForDatabaseReady(String containerId, Language language) {
            if (!(language instanceof DatabaseLanguage dbLang) || !dbLang.requiresDaemonWait()) {
                return;
            }
    
            List<String> readyCmd = dbLang.readinessCheckCommand();
            if (readyCmd == null || readyCmd.isEmpty()) {
                return;
            }
    
            long timeoutMs = executionProperties.getDatabase().getReadinessTimeoutMs();
            long pollIntervalMs = executionProperties.getDatabase().getReadinessPollIntervalMs();
            long deadline = System.currentTimeMillis() + timeoutMs;
    
            while (System.currentTimeMillis() < deadline) {
                if (!dockerExecutionService.isContainerRunning(containerId)) {
                    String containerLogs = dockerExecutionService.getContainerLogs(containerId, 50);
                    log.error("Database container [{}] stopped unexpectedly for language [{}]. Container logs:\n{}",
                            containerId, language.id(), containerLogs);
                    throw new DockerExecutionException("Database container stopped unexpectedly: "
                            + (containerLogs != null && !containerLogs.isBlank() ? containerLogs : "container exited with non-zero status"));
                }
    
                try {
                    DockerExecutionResult result = dockerExecutionService.execContainer(containerId, readyCmd);
                    if (result != null && result.exitCode() == 0L) {
                        return;
                    }
                } catch (Exception ignored) {
                }
                try {
                    Thread.sleep(pollIntervalMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            log.error("Database container [{}] did not become ready within timeout for language [{}]", containerId,
                    language.id());
            throw new DockerExecutionException("Database container [" + containerId + "] did not become ready within timeout for language ["
                    + language.id() + "]");
        }
    
        private static @NonNull List<String> getBashCompileCmd(CompiledLanguage language, List<String> fileNames, String boxDir) {
            List<String> compileCommand = language.compile(fileNames);
    
            List<String> bashCompileCmd = new ArrayList<>();
            bashCompileCmd.add("/bin/bash");
            bashCompileCmd.add("-c");
            bashCompileCmd.add("cd \"$1\" && shift && exec \"$@\"");
            bashCompileCmd.add("--");
            bashCompileCmd.add(boxDir);
            bashCompileCmd.addAll(compileCommand);
            return bashCompileCmd;
        }
    
        private static @NonNull List<String> getBashCompileCmd(CompiledLanguage language, String fileName, String boxDir) {
            return getBashCompileCmd(language, List.of(fileName), boxDir);
        }
    
        private List<String> buildExecutionLogs(IsolateExecutionResult result) {
            List<String> logs = new ArrayList<>();
            if (result == null) {
                return logs;
            }
    
            if (result.cpuTimeSeconds() != null) {
                logs.add("CPU Time: " + result.cpuTimeSeconds() + "s");
            }
            if (result.wallTimeSeconds() != null) {
                logs.add("Wall Time: " + result.wallTimeSeconds() + "s");
            }
            if (result.memoryKb() != null) {
                logs.add("Memory: " + result.memoryKb() + " KB");
            }
            return logs;
        }
    
        private List<String> wrapWithBash(List<String> command) {
            if (command == null || command.isEmpty()) {
                return List.of();
            }
            if (command.getFirst().equals("/bin/bash") || command.getFirst().equals("/bin/sh")) {
                return command;
            }
            List<String> wrapped = new ArrayList<>();
            wrapped.add("/bin/bash");
            wrapped.add("-c");
            wrapped.add("exec \"$@\"");
            wrapped.add("--");
            wrapped.addAll(command);
            return wrapped;
        }

        private String resolveCurrentUserId() {
            try {
                org.springframework.security.core.Authentication auth =
                        org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
                if (auth != null && auth.isAuthenticated() && auth.getName() != null && !"anonymousUser".equals(auth.getName())) {
                    return auth.getName();
                }
            } catch (Exception ignored) {
            }
            return null;
        }
    }
