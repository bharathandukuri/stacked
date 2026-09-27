package com.bharath.stacked.modules.judge.sandbox.drivers;

import com.bharath.stacked.modules.judge.config.JudgeProperties;
import com.bharath.stacked.modules.judge.enums.SandboxDriverType;
import com.bharath.stacked.modules.judge.sandbox.SandboxDriver;
import com.bharath.stacked.modules.judge.sandbox.model.SandboxExecutionRequest;
import com.bharath.stacked.modules.judge.sandbox.model.SandboxExecutionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Docker container-based sandbox driver providing process, network, and
 * resource isolation.
 * Supports connecting to local daemon, remote Docker host, or docker-compose
 * docker-sandbox service.
 */
@Component
public class DockerSandboxDriver implements SandboxDriver {

    private static final Logger log = LoggerFactory.getLogger(DockerSandboxDriver.class);

    private final JudgeProperties judgeProperties;

    public DockerSandboxDriver(JudgeProperties judgeProperties) {
        this.judgeProperties = judgeProperties;
    }

    @Override
    public SandboxDriverType getType() {
        return SandboxDriverType.DOCKER;
    }

    @Override
    public boolean isAvailable() {
        try {
            var dockerProps = judgeProperties.docker();
            List<String> args = new ArrayList<>();
            args.add(dockerProps.cliPath());
            if (dockerProps.host() != null && !dockerProps.host().isBlank()) {
                args.add("-H");
                args.add(dockerProps.host());
            }
            args.add("version");

            Process process = new ProcessBuilder(args).start();
            boolean finished = process.waitFor(3000, TimeUnit.MILLISECONDS);
            return finished && process.exitValue() == 0;
        } catch (Exception e) {
            log.debug("Docker CLI/daemon not available: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public SandboxExecutionResult execute(SandboxExecutionRequest request) {
        if (request.dockerImage() == null || request.dockerImage().isBlank()) {
            return SandboxExecutionResult.failure("Docker image not configured for language: " + request.languageId());
        }

        String runContainerName = "stacked-run-" + request.executionId();
        String compileContainerName = "stacked-compile-" + request.executionId();
        Path workspace = Path.of(judgeProperties.workspaceDir(), "docker-" + request.executionId());

        try {
            Files.createDirectories(workspace);

            // Write all files into isolated workspace
            for (var entry : request.files().entrySet()) {
                Path filePath = workspace.resolve(entry.getKey());
                if (filePath.getParent() != null) {
                    Files.createDirectories(filePath.getParent());
                }
                Files.writeString(filePath, entry.getValue(), StandardCharsets.UTF_8);
            }

            long totalStartTime = System.currentTimeMillis();

            // Step 1: Compilation inside Docker container (if applicable)
            if (request.compileCommand() != null && !request.compileCommand().isBlank()) {
                List<String> compileArgs = buildDockerCommand(
                        compileContainerName,
                        workspace,
                        request.dockerImage(),
                        request.memoryLimitMb(),
                        false,
                        "sh", "-c", request.compileCommand());

                ProcessBuilder compilePb = new ProcessBuilder(compileArgs);
                applyEnvironment(compilePb);
                Process compileProcess = compilePb.start();

                boolean compileFinished = compileProcess.waitFor(judgeProperties.compileTimeoutMs(),
                        TimeUnit.MILLISECONDS);
                if (!compileFinished) {
                    killContainer(compileContainerName);
                    compileProcess.destroyForcibly();
                    return SandboxExecutionResult.compileError(-1, "",
                            "Compilation timed out after " + judgeProperties.compileTimeoutMs() + "ms",
                            System.currentTimeMillis() - totalStartTime);
                }

                int compileExit = compileProcess.exitValue();
                if (compileExit != 0) {
                    String compileStdout = readStream(compileProcess.getInputStream(),
                            judgeProperties.maxOutputSizeBytes());
                    String compileStderr = readStream(compileProcess.getErrorStream(),
                            judgeProperties.maxOutputSizeBytes());
                    return SandboxExecutionResult.compileError(compileExit, compileStdout, compileStderr,
                            System.currentTimeMillis() - totalStartTime);
                }
            }

            // Step 2: Execution inside Docker container with resource & security
            // constraints
            long runStartTime = System.currentTimeMillis();
            List<String> runArgs = buildDockerCommand(
                    runContainerName,
                    workspace,
                    request.dockerImage(),
                    request.memoryLimitMb(),
                    true, // Interactive stdin
                    "sh", "-c", request.runCommand());

            ProcessBuilder runPb = new ProcessBuilder(runArgs);
            applyEnvironment(runPb);
            Process runProcess = runPb.start();

            // Feed stdin to the container
            if (request.stdin() != null && !request.stdin().isEmpty()) {
                try (OutputStream os = runProcess.getOutputStream()) {
                    os.write(request.stdin().getBytes(StandardCharsets.UTF_8));
                    os.flush();
                }
            } else {
                runProcess.getOutputStream().close();
            }

            boolean runFinished = runProcess.waitFor(request.timeoutMs(), TimeUnit.MILLISECONDS);
            long executionTime = System.currentTimeMillis() - runStartTime;

            if (!runFinished) {
                killContainer(runContainerName);
                runProcess.destroyForcibly();
                return SandboxExecutionResult.timeout(request.timeoutMs());
            }

            int exitCode = runProcess.exitValue();
            String stdout = readStream(runProcess.getInputStream(), judgeProperties.maxOutputSizeBytes());
            String stderr = readStream(runProcess.getErrorStream(), judgeProperties.maxOutputSizeBytes());

            if (exitCode != 0) {
                // Exit code 137 indicates OOM / SIGKILL in Docker
                if (exitCode == 137) {
                    return SandboxExecutionResult.memoryLimitExceeded(request.memoryLimitMb() * 1024L);
                }
                return SandboxExecutionResult.runtimeError(exitCode, stdout, stderr, executionTime);
            }

            return SandboxExecutionResult.success(exitCode, stdout, stderr, executionTime, 0);

        } catch (Exception e) {
            log.error("Docker execution failed for ID {}: {}", request.executionId(), e.getMessage(), e);
            return SandboxExecutionResult.failure("Docker execution error: " + e.getMessage());
        } finally {
            cleanupContainer(runContainerName);
            cleanupContainer(compileContainerName);
            cleanupDirectory(workspace);
        }
    }

    private List<String> buildDockerCommand(
            String containerName,
            Path workspace,
            String image,
            int memoryLimitMb,
            boolean interactive,
            String... command) {
        var dockerProps = judgeProperties.docker();
        List<String> args = new ArrayList<>();
        args.add(dockerProps.cliPath());

        // Connect to configured Docker host (e.g. tcp://localhost:2375)
        if (dockerProps.host() != null && !dockerProps.host().isBlank()) {
            args.add("-H");
            args.add(dockerProps.host());
        }

        args.add("run");
        args.add("--rm");

        if (interactive) {
            args.add("-i");
        }

        args.add("--name");
        args.add(containerName);

        // Security & Network isolation
        args.add("--network");
        args.add(dockerProps.network());

        // Resource limits
        args.add("--memory=" + memoryLimitMb + "m");
        args.add("--memory-swap=" + memoryLimitMb + "m");
        args.add("--cpus=" + dockerProps.cpus());
        args.add("--pids-limit=" + dockerProps.pidsLimit());

        // Mount isolated workspace directory
        args.add("-v");
        args.add(workspace.toAbsolutePath() + ":/workspace:rw");
        args.add("-w");
        args.add("/workspace");

        args.add(image);
        args.addAll(List.of(command));

        return args;
    }

    private void applyEnvironment(ProcessBuilder pb) {
        var dockerProps = judgeProperties.docker();
        if (dockerProps.host() != null && !dockerProps.host().isBlank()) {
            pb.environment().put("DOCKER_HOST", dockerProps.host());
        }
    }

    private void killContainer(String containerName) {
        try {
            var dockerProps = judgeProperties.docker();
            List<String> args = new ArrayList<>();
            args.add(dockerProps.cliPath());
            if (dockerProps.host() != null && !dockerProps.host().isBlank()) {
                args.add("-H");
                args.add(dockerProps.host());
            }
            args.add("kill");
            args.add(containerName);

            ProcessBuilder pb = new ProcessBuilder(args);
            applyEnvironment(pb);
            pb.start().waitFor(3000, TimeUnit.MILLISECONDS);
        } catch (Exception ignored) {
        }
    }

    private void cleanupContainer(String containerName) {
        try {
            var dockerProps = judgeProperties.docker();
            List<String> args = new ArrayList<>();
            args.add(dockerProps.cliPath());
            if (dockerProps.host() != null && !dockerProps.host().isBlank()) {
                args.add("-H");
                args.add(dockerProps.host());
            }
            args.add("rm");
            args.add("-f");
            args.add(containerName);

            ProcessBuilder pb = new ProcessBuilder(args);
            applyEnvironment(pb);
            pb.start().waitFor(2000, TimeUnit.MILLISECONDS);
        } catch (Exception ignored) {
        }
    }

    private String readStream(InputStream is, int maxBytes) {
        try (is; ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int total = 0;
            int read;
            while ((read = is.read(buffer)) != -1) {
                if (total + read > maxBytes) {
                    baos.write(buffer, 0, maxBytes - total);
                    baos.write("\n...[output truncated]...".getBytes(StandardCharsets.UTF_8));
                    break;
                }
                baos.write(buffer, 0, read);
                total += read;
            }
            return baos.toString(StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "";
        }
    }

    private void cleanupDirectory(Path dir) {
        if (!Files.exists(dir))
            return;
        try {
            Files.walk(dir)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        } catch (Exception e) {
            log.warn("Failed to cleanup directory {}: {}", dir, e.getMessage());
        }
    }
}
