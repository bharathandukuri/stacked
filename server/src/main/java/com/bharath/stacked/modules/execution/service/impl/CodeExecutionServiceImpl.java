package com.bharath.stacked.modules.execution.service.impl;

import com.bharath.stacked.modules.execution.dto.*;
import com.bharath.stacked.modules.execution.dto.request.SimpleCodeExecutionRequest;
import com.bharath.stacked.modules.execution.dto.response.DockerExecutionResult;
import com.bharath.stacked.modules.execution.dto.response.IsolateExecutionResult;
import com.bharath.stacked.modules.execution.dto.response.SimpleCodeExecutionResult;
import com.bharath.stacked.modules.execution.enums.CodeExecutionStatus;
import com.bharath.stacked.modules.execution.exception.DockerExecutionException;
import com.bharath.stacked.modules.execution.mapper.CodeExecutionStatusMapper;
import com.bharath.stacked.modules.execution.service.CodeExecutionService;
import com.bharath.stacked.modules.execution.service.DockerExecutionService;
import com.bharath.stacked.modules.execution.service.IsolateExecutionService;
import com.bharath.stacked.modules.language.CompiledLanguage;
import com.bharath.stacked.modules.language.Language;
import com.bharath.stacked.modules.language.LanguageType;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class CodeExecutionServiceImpl implements CodeExecutionService {

    private final DockerExecutionService dockerExecutionService;
    private final IsolateExecutionService isolateExecutionService;
    private final CodeExecutionStatusMapper codeExecutionStatusMapper;

    public CodeExecutionServiceImpl(
            DockerExecutionService dockerExecutionService,
            IsolateExecutionService isolateExecutionService,
            CodeExecutionStatusMapper codeExecutionStatusMapper) {
        super();
        this.dockerExecutionService = dockerExecutionService;
        this.isolateExecutionService = isolateExecutionService;
        this.codeExecutionStatusMapper = codeExecutionStatusMapper;
    }

    @Override
    public SimpleCodeExecutionResult run(SimpleCodeExecutionRequest request) {
        if (request == null || request.getLanguage() == null) {
            return SimpleCodeExecutionResult.builder()
                    .executionStatus(CodeExecutionStatus.SYSTEM_ERROR)
                    .logs(List.of("Execution request and language must not be null."))
                    .build();
        }

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

            String fileName = resolveFileName(request);
            String code = request.getCode() != null ? request.getCode() : "";

            dockerExecutionService.writeFile(
                    dockerContainer.id(),
                    boxDir + "/" + fileName,
                    code);

            if (language.type() == LanguageType.COMPILED) {
                List<String> bashCompileCmd = getBashCompileCmd((CompiledLanguage) language, fileName, boxDir);

                DockerExecutionResult compileResult = dockerExecutionService.execContainer(
                        dockerContainer.id(),
                        bashCompileCmd);

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

            IsolateExecutionConstraints isolateConstraints = mapConstraints(language, request.getConstraints());

            List<String> runCommand = wrapWithBash(language.run(fileName));

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
            DatabaseContainerConstraints constraints = DatabaseContainerConstraints.defaults();
            dockerContainer = dockerExecutionService.createContainer(dockerImage, constraints);
            dockerExecutionService.startContainer(dockerContainer.id());
            waitForDatabaseReady(dockerContainer.id(), language);

            String fileName = resolveFileName(request);
            String code = request.getCode() != null ? request.getCode() : "";
            String targetPath = "/tmp/" + fileName;

            dockerExecutionService.writeFile(
                    dockerContainer.id(),
                    targetPath,
                    code);

            long timeLimitMs = (request.getConstraints() != null && request.getConstraints().timeLimitMs() > 0)
                    ? request.getConstraints().timeLimitMs()
                    : 5000L;

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

    private static @NonNull CodeExecutionStatus getCodeExecutionStatus(DockerExecutionResult execResult, String cleanStderr) {
        CodeExecutionStatus status;
        if (execResult.exitCode() == 0L) {
            if (cleanStderr != null && !cleanStderr.isBlank()
                    && (cleanStderr.contains("ERROR:") || cleanStderr.contains("ERROR "))) {
                status = CodeExecutionStatus.RUNTIME_ERROR;
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
        String langId = language != null && language.id() != null ? language.id().toLowerCase() : "";
        List<String> readyCmd;
        if (langId.contains("postgres")) {
            readyCmd = List.of("psql", "-U", "postgres", "-d", "stacked_judge_db", "-c", "SELECT 1;");
        } else if (langId.contains("mysql")) {
            readyCmd = List.of("mysql", "-S", "/var/run/mysqld/mysqld.sock", "stacked_judge_db", "-e", "SELECT 1;");
        } else if (langId.contains("mongo")) {
            readyCmd = List.of("mongosh", "--quiet", "--eval", "db.adminCommand('ping')");
        } else if (langId.contains("sqlite")) {
            return;
        } else {
            return;
        }

        long deadline = System.currentTimeMillis() + 30000L;
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

                Thread.sleep(150L);
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

    private static @NonNull List<String> getBashCompileCmd(CompiledLanguage language, String fileName, String boxDir) {
        List<String> compileCommand = language.compile(fileName);

        List<String> bashCompileCmd = new ArrayList<>();
        bashCompileCmd.add("/bin/bash");
        bashCompileCmd.add("-c");
        bashCompileCmd.add("cd \"$1\" && shift && exec \"$@\"");
        bashCompileCmd.add("--");
        bashCompileCmd.add(boxDir);
        bashCompileCmd.addAll(compileCommand);
        return bashCompileCmd;
    }

    private String resolveFileName(SimpleCodeExecutionRequest request) {
        if (request.getFileName() != null && !request.getFileName().isBlank()) {
            return request.getFileName();
        }

        Language language = request.getLanguage();
        if ("java-21".equalsIgnoreCase(language.id())) {
            return "Main.java";
        }

        String ext = language.fileExtension() != null ? language.fileExtension() : "";
        return "main" + ext;
    }

    private IsolateExecutionConstraints mapConstraints(Language language, CodeExecutionConstraints constraints) {
        double cpuTime = 2.0;
        double wallTime = 5.0;
        Long memoryKb = 262144L;

        if (constraints != null) {
            if (constraints.timeLimitMs() > 0) {
                cpuTime = constraints.timeLimitMs() / 1000.0;
                wallTime = Math.max(cpuTime * 2.0, cpuTime + 1.0);
            }
            if (constraints.memoryLimitKb() != null && constraints.memoryLimitKb() > 0) {
                memoryKb = constraints.memoryLimitKb();
            }
        }

        // Managed runtimes (JVM, V8, Go) pre-allocate large virtual address spaces
        // for JIT code caches, GC card tables, and pointer compression. Setting RLIMIT_AS (--mem)
        // starves virtual address space and causes initialization crashes.
        String langId = language != null && language.id() != null ? language.id().toLowerCase() : "";
        if (langId.contains("java") || langId.contains("node") || langId.contains("javascript")
                || langId.contains("kotlin") || langId.contains("typescript")
                || langId.contains("go")) {
            memoryKb = null;
        }

        Long fileSizeKb = 10240L;

        return new IsolateExecutionConstraints(
                cpuTime,
                wallTime,
                memoryKb,
                50,
                fileSizeKb);
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
}