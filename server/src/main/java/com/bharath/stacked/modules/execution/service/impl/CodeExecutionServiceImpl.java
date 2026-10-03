package com.bharath.stacked.modules.execution.service.impl;

import com.bharath.stacked.modules.execution.dto.CodeExecutionConstraints;
import com.bharath.stacked.modules.execution.dto.DockerContainerDetails;
import com.bharath.stacked.modules.execution.dto.DockerImageDetails;
import com.bharath.stacked.modules.execution.dto.IsolateExecutionConstraints;
import com.bharath.stacked.modules.execution.dto.IsolateSandBoxDetails;
import com.bharath.stacked.modules.execution.dto.request.SimpleCodeExecutionRequest;
import com.bharath.stacked.modules.execution.dto.response.DockerExecutionResult;
import com.bharath.stacked.modules.execution.dto.response.IsolateExecutionResult;
import com.bharath.stacked.modules.execution.dto.response.SimpleCodeExecutionResult;
import com.bharath.stacked.modules.execution.enums.CodeExecutionStatus;
import com.bharath.stacked.modules.execution.mapper.CodeExecutionStatusMapper;
import com.bharath.stacked.modules.execution.service.CodeExecutionService;
import com.bharath.stacked.modules.execution.service.DockerExecutionService;
import com.bharath.stacked.modules.execution.service.IsolateExecutionService;
import com.bharath.stacked.modules.language.CompiledLanguage;
import com.bharath.stacked.modules.language.Language;
import com.bharath.stacked.modules.language.LanguageType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class CodeExecutionServiceImpl implements CodeExecutionService {

    private final DockerExecutionService dockerExecutionService;
    private final IsolateExecutionService isolateExecutionService;
    private final CodeExecutionStatusMapper codeExecutionStatusMapper;

    @Override
    public SimpleCodeExecutionResult run(SimpleCodeExecutionRequest request) {
        if (request == null || request.getLanguage() == null) {
            return SimpleCodeExecutionResult.builder()
                    .executionStatus(CodeExecutionStatus.SYSTEM_ERROR)
                    .logs(List.of("Execution request and language must not be null."))
                    .build();
        }

        DockerContainerDetails dockerContainer = null;
        IsolateSandBoxDetails sandBoxDetails = null;

        try {
            Language language = request.getLanguage();
            DockerImageDetails dockerImage = language.dockerImageDetails();

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

            List<String> logs = buildExecutionLogs(isolateResult);

            return SimpleCodeExecutionResult.builder()
                    .stdout(isolateResult.stdout())
                    .stderr(isolateResult.stderr())
                    .exitCode(isolateResult.exitCode())
                    .exitSignal(isolateResult.exitSignal())
                    .executionStatus(codeExecutionStatusMapper.map(isolateResult.status()))
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
            return "Solution.java";
        }

        String ext = language.fileExtension() != null ? language.fileExtension() : "";
        return "solution" + ext;
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
            if (constraints.memoryLimitKb() > 0) {
                memoryKb = constraints.memoryLimitKb();
            }
        }

        // JVM and Node.js V8 runtimes pre-allocate large virtual address spaces
        // for JIT code caches, GC card tables, and pointer compression. Setting RLIMIT_AS (--mem)
        // starves virtual memory and causes initialization crash. We rely on container memory isolation.
        String langId = language != null && language.id() != null ? language.id().toLowerCase() : "";
        if (langId.contains("java") || langId.contains("node") || langId.contains("javascript")) {
            memoryKb = null;
        }

        return new IsolateExecutionConstraints(
                cpuTime,
                wallTime,
                memoryKb,
                50,
                10240L);
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
