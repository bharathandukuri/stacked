package com.bharath.stacked.modules.judge.sandbox.drivers;

import com.bharath.stacked.modules.judge.config.JudgeProperties;
import com.bharath.stacked.modules.judge.enums.ExecutionStatus;
import com.bharath.stacked.modules.judge.enums.SandboxDriverType;
import com.bharath.stacked.modules.judge.sandbox.SandboxDriver;
import com.bharath.stacked.modules.judge.sandbox.model.SandboxExecutionRequest;
import com.bharath.stacked.modules.judge.sandbox.model.SandboxExecutionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Firecracker MicroVM-based sandbox driver using Linux KVM hardware
 * virtualization.
 * Supports executing workloads via:
 * 1. Docker Compose firecracker-sandbox service over REST API
 * (judge.sandbox.firecracker.service-url)
 * 2. Direct local Firecracker daemon process via Unix Domain Sockets using
 * native Java 25 NIO.
 */
@Component
public class FirecrackerSandboxDriver implements SandboxDriver {

    private static final Logger log = LoggerFactory.getLogger(FirecrackerSandboxDriver.class);

    private final JudgeProperties judgeProperties;
    private final RestClient restClient;

    public FirecrackerSandboxDriver(JudgeProperties judgeProperties) {
        this.judgeProperties = judgeProperties;
        this.restClient = RestClient.builder().build();
    }

    @Override
    public SandboxDriverType getType() {
        return SandboxDriverType.FIRECRACKER;
    }

    @Override
    public boolean isAvailable() {
        // 1. Check if Docker Compose firecracker-sandbox service is responsive
        if (isRemoteServiceHealthy()) {
            return true;
        }

        // 2. Check if local host has firecracker binary and KVM privileges
        try {
            var fcProps = judgeProperties.firecracker();
            Path binPath = Path.of(fcProps.binaryPath());
            if (!Files.isExecutable(binPath)) {
                return false;
            }

            File kvm = new File("/dev/kvm");
            return kvm.exists() && kvm.canRead() && kvm.canWrite();
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isRemoteServiceHealthy() {
        try {
            String serviceUrl = judgeProperties.firecracker().serviceUrl();
            if (serviceUrl == null || serviceUrl.isBlank())
                return false;

            var response = restClient.get()
                    .uri(serviceUrl + "/health")
                    .retrieve()
                    .toBodilessEntity();
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.debug("Remote Firecracker service not reachable at {}: {}",
                    judgeProperties.firecracker().serviceUrl(), e.getMessage());
            return false;
        }
    }

    @Override
    public SandboxExecutionResult execute(SandboxExecutionRequest request) {
        // Priority 1: If docker-compose firecracker-sandbox service is reachable, route
        // to it
        if (isRemoteServiceHealthy()) {
            return executeViaRemoteService(request);
        }

        // Priority 2: Direct local KVM/Firecracker execution if available
        var fcProps = judgeProperties.firecracker();
        File kvm = new File("/dev/kvm");
        boolean localKvmReady = kvm.exists() && kvm.canRead() && kvm.canWrite() &&
                Files.isExecutable(Path.of(fcProps.binaryPath()));

        if (localKvmReady) {
            return executeViaLocalDaemon(request);
        }

        return SandboxExecutionResult.failure(
                "Firecracker sandboxing is not available. The docker-compose firecracker-sandbox service " +
                        "is not reachable at " + fcProps.serviceUrl() + ", and local /dev/kvm or binary is missing. " +
                        "Run 'docker compose up -d firecracker-sandbox' or set judge.sandbox.driver=DOCKER.");
    }

    private SandboxExecutionResult executeViaRemoteService(SandboxExecutionRequest request) {
        try {
            String serviceUrl = judgeProperties.firecracker().serviceUrl();
            log.info("Dispatching execution {} to Firecracker service at {}", request.executionId(), serviceUrl);

            RemoteExecutionResponse response = restClient.post()
                    .uri(serviceUrl + "/execute")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(RemoteExecutionResponse.class);

            if (response == null) {
                return SandboxExecutionResult.failure("Empty response received from Firecracker service");
            }

            ExecutionStatus status;
            try {
                status = response.status() != null
                        ? ExecutionStatus.valueOf(response.status())
                        : (response.exitCode() == 0 ? ExecutionStatus.SUCCESS : ExecutionStatus.RUNTIME_ERROR);
            } catch (IllegalArgumentException e) {
                status = response.exitCode() == 0 ? ExecutionStatus.SUCCESS : ExecutionStatus.RUNTIME_ERROR;
            }

            return new SandboxExecutionResult(
                    response.exitCode(),
                    response.stdout() != null ? response.stdout() : "",
                    response.stderr() != null ? response.stderr() : "",
                    response.executionTimeMs(),
                    response.memoryUsedKb(),
                    status,
                    response.errorMessage());

        } catch (Exception e) {
            log.error("Failed to execute via remote Firecracker service: {}", e.getMessage(), e);
            return SandboxExecutionResult.failure("Remote Firecracker execution error: " + e.getMessage());
        }
    }

    private record RemoteExecutionResponse(
            int exitCode,
            String stdout,
            String stderr,
            long executionTimeMs,
            long memoryUsedKb,
            String status,
            String errorMessage) {
    }

    private SandboxExecutionResult executeViaLocalDaemon(SandboxExecutionRequest request) {
        var fcProps = judgeProperties.firecracker();

        String rootfsName = request.firecrackerRootfs() != null
                ? request.firecrackerRootfs()
                : "rootfs-" + request.languageId() + ".ext4";
        Path rootfsPath = Path.of(fcProps.rootfsDir(), rootfsName);

        if (!Files.exists(rootfsPath)) {
            return SandboxExecutionResult.failure(
                    "Firecracker rootfs image not found at: " + rootfsPath.toAbsolutePath() +
                            ". Place rootfs in " + fcProps.rootfsDir());
        }

        Path kernelPath = Path.of(fcProps.kernelPath());
        if (!Files.exists(kernelPath)) {
            return SandboxExecutionResult.failure(
                    "Firecracker kernel image (vmlinux) not found at: " + kernelPath.toAbsolutePath());
        }

        Path socketDir = Path.of(fcProps.socketDir());
        Path socketPath = socketDir.resolve("fc-" + request.executionId() + ".sock");
        Path workspace = Path.of(judgeProperties.workspaceDir(), "fc-" + request.executionId());

        Process firecrackerProcess = null;

        try {
            Files.createDirectories(socketDir);
            Files.createDirectories(workspace);
            Files.deleteIfExists(socketPath);

            for (var entry : request.files().entrySet()) {
                Path filePath = workspace.resolve(entry.getKey());
                if (filePath.getParent() != null) {
                    Files.createDirectories(filePath.getParent());
                }
                Files.writeString(filePath, entry.getValue(), StandardCharsets.UTF_8);
            }

            if (request.stdin() != null) {
                Files.writeString(workspace.resolve("stdin.txt"), request.stdin(), StandardCharsets.UTF_8);
            }

            ProcessBuilder pb = new ProcessBuilder(fcProps.binaryPath(), "--api-sock", socketPath.toString());
            pb.directory(workspace.toFile());
            firecrackerProcess = pb.start();

            boolean socketReady = waitForSocket(socketPath, 2000);
            if (!socketReady) {
                return SandboxExecutionResult.failure("Firecracker API socket did not become ready in time");
            }

            long startTime = System.currentTimeMillis();

            sendUnixSocketRequest(socketPath, "PUT", "/boot-source",
                    """
                            {
                              "kernel_image_path": "%s",
                              "boot_args": "console=ttyS0 reboot=k panic=1 pci=off init=/init"
                            }
                            """.formatted(escapeJson(kernelPath.toAbsolutePath().toString())));

            sendUnixSocketRequest(socketPath, "PUT", "/machine-config",
                    """
                            {
                              "vcpu_count": %d,
                              "mem_size_mib": %d
                            }
                            """.formatted(fcProps.vcpuCount(),
                            Math.min(request.memoryLimitMb(), fcProps.memSizeMib())));

            sendUnixSocketRequest(socketPath, "PUT", "/drives/rootfs",
                    """
                            {
                              "drive_id": "rootfs",
                              "path_on_host": "%s",
                              "is_root_device": true,
                              "is_read_only": false
                            }
                            """.formatted(escapeJson(rootfsPath.toAbsolutePath().toString())));

            sendUnixSocketRequest(socketPath, "PUT", "/actions",
                    """
                            {
                              "action_type": "InstanceStart"
                            }
                            """);

            boolean finished = firecrackerProcess.waitFor(request.timeoutMs(), TimeUnit.MILLISECONDS);
            long executionTime = System.currentTimeMillis() - startTime;

            if (!finished) {
                firecrackerProcess.destroyForcibly();
                return SandboxExecutionResult.timeout(request.timeoutMs());
            }

            int exitCode = firecrackerProcess.exitValue();
            String stdout = readStream(firecrackerProcess.getInputStream(), judgeProperties.maxOutputSizeBytes());
            String stderr = readStream(firecrackerProcess.getErrorStream(), judgeProperties.maxOutputSizeBytes());

            if (exitCode != 0) {
                return SandboxExecutionResult.runtimeError(exitCode, stdout, stderr, executionTime);
            }

            return SandboxExecutionResult.success(exitCode, stdout, stderr, executionTime, 0);

        } catch (Exception e) {
            log.error("Firecracker execution error for ID {}: {}", request.executionId(), e.getMessage(), e);
            return SandboxExecutionResult.failure("Firecracker execution error: " + e.getMessage());
        } finally {
            if (firecrackerProcess != null && firecrackerProcess.isAlive()) {
                firecrackerProcess.destroyForcibly();
            }
            try {
                Files.deleteIfExists(socketPath);
            } catch (Exception ignored) {
            }
            cleanupDirectory(workspace);
        }
    }

    private boolean waitForSocket(Path socketPath, long timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            if (Files.exists(socketPath)) {
                return true;
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    private void sendUnixSocketRequest(Path socketPath, String method, String path, String body) throws IOException {
        UnixDomainSocketAddress address = UnixDomainSocketAddress.of(socketPath);
        try (SocketChannel channel = SocketChannel.open(StandardProtocolFamily.UNIX)) {
            channel.connect(address);

            byte[] bodyBytes = body != null ? body.getBytes(StandardCharsets.UTF_8) : new byte[0];
            String httpRequest = method + " " + path + " HTTP/1.1\r\n" +
                    "Host: localhost\r\n" +
                    "Content-Type: application/json\r\n" +
                    "Content-Length: " + bodyBytes.length + "\r\n" +
                    "Connection: close\r\n\r\n";

            channel.write(ByteBuffer.wrap(httpRequest.getBytes(StandardCharsets.UTF_8)));
            if (bodyBytes.length > 0) {
                channel.write(ByteBuffer.wrap(bodyBytes));
            }

            ByteBuffer buffer = ByteBuffer.allocate(1024);
            int bytesRead = channel.read(buffer);
            if (bytesRead > 0) {
                buffer.flip();
                String response = StandardCharsets.UTF_8.decode(buffer).toString();
                if (!response.contains("200 OK") && !response.contains("204 No Content")) {
                    log.warn("Firecracker API returned non-2xx for {} {}: {}", method, path, response);
                }
            }
        }
    }

    private String escapeJson(String input) {
        if (input == null)
            return "";
        return input.replace("\\", "\\\\").replace("\"", "\\\"");
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
