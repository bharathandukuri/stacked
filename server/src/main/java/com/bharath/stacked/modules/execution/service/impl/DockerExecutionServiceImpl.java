package com.bharath.stacked.modules.execution.service.impl;

import com.bharath.stacked.modules.execution.config.DockerProperties;
import com.bharath.stacked.modules.execution.dto.DockerContainerConstraints;
import com.bharath.stacked.modules.execution.dto.DockerContainerDetails;
import com.bharath.stacked.modules.execution.dto.DockerImageDetails;
import com.bharath.stacked.modules.execution.dto.ManagedContainer;
import com.bharath.stacked.modules.execution.dto.response.DockerExecutionResult;
import com.bharath.stacked.modules.execution.exception.*;
import com.bharath.stacked.modules.execution.registry.DockerImageRegistry;
import com.bharath.stacked.modules.execution.registry.ManagedContainerRegistry;
import com.bharath.stacked.modules.execution.service.DockerExecutionService;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.BuildResponseItem;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.StreamType;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class DockerExecutionServiceImpl implements DockerExecutionService {

    private final DockerClient dockerClient;
    private final DockerProperties dockerProperties;
    private final ManagedContainerRegistry containerRegistry;

    private static class ExecOutputCallback
            extends ResultCallback.Adapter<Frame> {

        private final ByteArrayOutputStream stdout =
                new ByteArrayOutputStream();

        private final ByteArrayOutputStream stderr =
                new ByteArrayOutputStream();

        @Override
        public void onNext(Frame frame) {
            try {
                if (frame.getStreamType() == StreamType.STDOUT) {
                    stdout.write(frame.getPayload());

                } else if (frame.getStreamType() == StreamType.STDERR) {
                    stderr.write(frame.getPayload());

                } else if (frame.getStreamType() == StreamType.RAW) {
                    stdout.write(frame.getPayload());
                }

            } catch (IOException e) {
                onError(e);
            }
        }

        public String stdout() {
            return stdout.toString(StandardCharsets.UTF_8);
        }

        public String stderr() {
            return stderr.toString(StandardCharsets.UTF_8);
        }
    }

    @PostConstruct
    void validateImages() {
        if (!dockerProperties.isEnabled()) {
            log.info("Docker execution is disabled; skipping image validation.");
            return;
        }

        ensureSandboxNetworkExists();

        for (DockerImageRegistry registry : DockerImageRegistry.values()) {
            try {
                DockerImageDetails image = registry.dockerImage();

                if (isImageExists(image)) {
                    log.info(
                            "Verified required Docker image [{}] is present.",
                            image.reference()
                    );
                    continue;
                }

                log.info(
                        "Required Docker image [{}] is missing. Creating image.",
                        image.reference()
                );

                createImage(image);
            } catch (Exception e) {
                log.warn("Could not prepare Docker image [{}]: {}. Continuing startup.", registry.name(), e.getMessage());
            }
        }
    }

    @Override
    public boolean isImageExists(DockerImageDetails dockerImageDetails) {
        try {
            dockerClient
                    .inspectImageCmd(dockerImageDetails.reference())
                    .exec();

            return true;

        } catch (NotFoundException e) {
            return false;
        }
    }

    @Override
    public void createImage(DockerImageDetails dockerImageDetails) throws DockerImageCreationException {
        try {
            ClassPathResource resource =
                    new ClassPathResource(dockerImageDetails.resourcePath());

            if (!resource.exists()) {
                throw new IllegalArgumentException(
                        "Docker build context does not exist: "
                                + dockerImageDetails.resourcePath()
                );
            }

            File buildContext = resource.getFile();

            if (!buildContext.isDirectory()) {
                throw new IllegalArgumentException(
                        "Docker build context is not a directory: "
                                + buildContext.getAbsolutePath()
                );
            }

            log.info(
                    "Building Docker image [{}] from [{}]",
                    dockerImageDetails.reference(),
                    buildContext.getAbsolutePath()
            );

            BuildImageResultCallback callback = new BuildImageResultCallback() {

                @Override
                public void onNext(BuildResponseItem item) {
                    if (item.getStream() != null) {
                        log.info("[Docker Build] {}", item.getStream().trim());
                    }

                    if (item.getErrorDetail() != null) {
                        log.error("[Docker Build] {}", item.getErrorDetail());
                    }

                    if (item.getErrorDetail() != null) {
                        log.error(
                                "[Docker Build] {}",
                                item.getErrorDetail().getMessage()
                        );
                    }

                    super.onNext(item);
                }
            };

            dockerClient
                    .buildImageCmd(buildContext)
                    .withTags(Set.of(dockerImageDetails.reference()))
                    .exec(callback)
                    .awaitImageId();

            if (!isImageExists(dockerImageDetails)) {
                throw new IllegalStateException(
                        "Docker image build completed but image was not found: "
                                + dockerImageDetails.reference()
                );
            }

            log.info(
                    "Successfully created Docker image [{}].",
                    dockerImageDetails.reference()
            );

        } catch (Exception e) {
            log.error(
                    "Failed to create Docker image [{}].",
                    dockerImageDetails.reference(),
                    e
            );

            throw new DockerImageCreationException(
                    "Failed to create Docker image: "
                            + dockerImageDetails.reference(),
                    e
            );
        }
    }

    @Override
    public DockerContainerDetails createContainer(
            DockerImageDetails dockerImageDetails
    ) throws DockerContainerCreationException {
        return createContainer(dockerImageDetails, null);
    }

    public static final String SANDBOX_NETWORK_NAME = "stacked-sandbox-net";

    private void ensureSandboxNetworkExists() {
        try {
            boolean exists = dockerClient.listNetworksCmd()
                    .withNameFilter(SANDBOX_NETWORK_NAME)
                    .exec()
                    .stream()
                    .anyMatch(n -> SANDBOX_NETWORK_NAME.equals(n.getName()));

            if (!exists) {
                dockerClient.createNetworkCmd()
                        .withName(SANDBOX_NETWORK_NAME)
                        .withDriver("bridge")
                        .withInternal(true)
                        .withCheckDuplicate(true)
                        .exec();
                log.info("Created isolated internal Docker network [{}] for execution sandboxes.", SANDBOX_NETWORK_NAME);
            }
        } catch (Exception e) {
            log.warn("Could not ensure sandbox network [{}] exists: {}", SANDBOX_NETWORK_NAME, e.getMessage());
        }
    }

    @Override
    public DockerContainerDetails createContainer(
            DockerImageDetails dockerImageDetails,
            DockerContainerConstraints constraints
    ) throws DockerContainerCreationException {

        String image = dockerImageDetails.reference();

        if (!isImageExists(dockerImageDetails)) {
            throw new DockerContainerCreationException(
                    "Docker image does not exist: " + image
            );
        }

        String containerName = "stacked-execution-" + UUID.randomUUID();

        try {
            HostConfig hostConfig = HostConfig.newHostConfig()
                    .withPrivileged(true);

            boolean networkDisabled =
                    constraints == null || constraints.networkDisabled();

            DockerContainerConstraints effectiveConstraints = constraints != null
                    ? constraints
                    : DockerContainerConstraints.defaults();

            if (effectiveConstraints.cpuLimit() > 0) {
                long nanoCpus = Math.multiplyExact(
                        effectiveConstraints.cpuLimit(),
                        1_000_000_000L
                );

                hostConfig.withNanoCPUs(nanoCpus);
            }

            if (effectiveConstraints.memoryLimitKb() > 0) {
                long memoryBytes = Math.multiplyExact(
                        effectiveConstraints.memoryLimitKb(),
                        1024L
                );

                hostConfig.withMemory(memoryBytes);
                hostConfig.withMemorySwap(memoryBytes);
            }

            if (effectiveConstraints.pidsLimit() > 0) {
                hostConfig.withPidsLimit(effectiveConstraints.pidsLimit());
            }

            if (networkDisabled) {
                hostConfig.withNetworkMode("none");
            } else {
                ensureSandboxNetworkExists();
                hostConfig.withNetworkMode(SANDBOX_NETWORK_NAME);
            }

            CreateContainerCmd createCmd = dockerClient
                    .createContainerCmd(image)
                    .withName(containerName)
                    .withTty(true)
                    .withHostConfig(hostConfig);

            if (networkDisabled) {
                createCmd.withNetworkDisabled(true);
            }

            CreateContainerResponse response = createCmd.exec();
            String containerId = response.getId();

            ManagedContainer managedContainer = new ManagedContainer(
                    containerId,
                    containerName,
                    image,
                    Instant.now()
            );

            try {
                containerRegistry.register(managedContainer);
            } catch (Exception registrationException) {
                // Do not leave an untracked container behind.
                try {
                    dockerClient.removeContainerCmd(containerId)
                            .withForce(true)
                            .exec();
                } catch (Exception cleanupException) {
                    registrationException.addSuppressed(cleanupException);

                    log.error(
                            "Failed to clean up container [{}] after registry failure.",
                            containerId,
                            cleanupException
                    );
                }

                throw new DockerContainerCreationException(
                        "Failed to register created container with Stacked.",
                        registrationException
                );
            }

            log.info(
                    "Created Docker container [{}] from image [{}] with constraints [{}].",
                    containerName,
                    image,
                    constraints
            );

            return new DockerContainerDetails(containerId, containerName);

        } catch (DockerContainerCreationException e) {
            // Preserve the original exception and cause.
            throw e;
        } catch (Exception e) {
            log.error(
                    "Failed to create Docker container from image [{}].",
                    image,
                    e
            );

            throw new DockerContainerCreationException(
                    "Failed to create Docker container from image: " + image,
                    e
            );
        }
    }

    private void requireManagedContainer(String containerId) {
        if (containerId == null
                || !containerRegistry.isManaged(containerId)) {
            throw new DockerContainerNotFoundException(
                    "Container is not managed by Stacked."
            );
        }
    }

    @Override
    public boolean isContainerExists(String containerId) {
        try {
            dockerClient
                    .inspectContainerCmd(containerId)
                    .exec();

            return true;

        } catch (NotFoundException e) {
            return false;
        }
    }

    @Override
    public boolean isContainerRunning(String containerId) {
        try {
            InspectContainerResponse response = dockerClient.inspectContainerCmd(containerId).exec();
            return response != null && response.getState() != null && Boolean.TRUE.equals(response.getState().getRunning());
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String getContainerLogs(String containerId, int tailLines) {
        requireManagedContainer(containerId);
        try {
            StringBuilder sb = new StringBuilder();
            dockerClient.logContainerCmd(containerId)
                    .withStdOut(true)
                    .withStdErr(true)
                    .withTail(tailLines)
                    .exec(new ResultCallback.Adapter<Frame>() {
                        @Override
                        public void onNext(Frame frame) {
                            if (frame != null && frame.getPayload() != null) {
                                sb.append(new String(frame.getPayload(), StandardCharsets.UTF_8));
                            }
                        }
                    })
                    .awaitCompletion(2, TimeUnit.SECONDS);
            return sb.toString().trim();
        } catch (Exception e) {
            return "Unable to retrieve container logs: " + e.getMessage();
        }
    }

    @Override
    public void startContainer(String containerId)
            throws DockerContainerStartException {
        requireManagedContainer(containerId);

        if (!isContainerExists(containerId)) {
            throw new DockerContainerNotFoundException(
                    "Docker container does not exist: " + containerId
            );
        }

        try {
            dockerClient
                    .startContainerCmd(containerId)
                    .exec();

            log.info(
                    "Started Docker container [{}].",
                    containerId
            );

        } catch (Exception e) {
            log.error(
                    "Failed to start Docker container [{}].",
                    containerId,
                    e
            );

            throw new DockerContainerStartException(
                    "Failed to start Docker container: " + containerId,
                    e
            );
        }
    }

    @Override
    public void stopContainer(String containerId)
            throws DockerContainerStopException {
        requireManagedContainer(containerId);

        if (!isContainerExists(containerId)) {
            throw new DockerContainerNotFoundException(
                    "Docker container does not exist: " + containerId
            );
        }

        try {
            dockerClient
                    .stopContainerCmd(containerId)
                    .withTimeout(1)
                    .exec();

            log.info(
                    "Stopped Docker container [{}].",
                    containerId
            );

        } catch (Exception e) {
            log.error(
                    "Failed to stop Docker container [{}].",
                    containerId,
                    e
            );

            throw new DockerContainerStopException(
                    "Failed to stop Docker container: " + containerId,
                    e
            );
        }
    }

    @Override
    public DockerExecutionResult execContainer(
            String containerId,
            List<String> command
    ) throws DockerExecutionException {
        return execContainer(containerId, command, null);
    }

    @Override
    public DockerExecutionResult execContainer(
            String containerId,
            List<String> command,
            Long timeLimitMs
    ) throws DockerExecutionException {
        requireManagedContainer(containerId);

        if (!isContainerExists(containerId)) {
            throw new DockerContainerNotFoundException(
                    "Docker container does not exist: " + containerId
            );
        }

        try {
            ExecCreateCmdResponse exec = dockerClient
                    .execCreateCmd(containerId)
                    .withCmd(command.toArray(String[]::new))
                    .withAttachStdout(true)
                    .withAttachStderr(true)
                    .exec();

            ExecOutputCallback callback = new ExecOutputCallback();

            boolean completed;
            if (timeLimitMs != null && timeLimitMs > 0) {
                completed = dockerClient
                        .execStartCmd(exec.getId())
                        .withDetach(false)
                        .exec(callback)
                        .awaitCompletion(timeLimitMs, TimeUnit.MILLISECONDS);
            } else {
                dockerClient
                        .execStartCmd(exec.getId())
                        .withDetach(false)
                        .exec(callback)
                        .awaitCompletion();
                completed = true;
            }

            if (!completed) {
                log.warn("Command execution timed out after {} ms in container [{}]", timeLimitMs, containerId);
                return new DockerExecutionResult(
                        124L,
                        callback.stdout(),
                        callback.stderr() + (callback.stderr().isEmpty() ? "" : "\n") + "Command timed out after " + timeLimitMs + " ms."
                );
            }

            Long exitCode = dockerClient
                    .inspectExecCmd(exec.getId())
                    .exec()
                    .getExitCodeLong();

            return new DockerExecutionResult(
                    exitCode == null ? -1 : exitCode,
                    callback.stdout(),
                    callback.stderr()
            );

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

            throw new DockerExecutionException(
                    "Docker command execution was interrupted.",
                    e
            );

        } catch (Exception e) {

            log.error(
                    "Failed to execute command [{}] in container [{}].",
                    command,
                    containerId,
                    e
            );

            throw new DockerExecutionException(
                    "Failed to execute command in Docker container: "
                            + containerId,
                    e
            );
        }
    }

    @Override
    public String readFile(
            String containerId,
            String path
    ) throws DockerExecutionException {

        DockerExecutionResult result =
                execContainer(
                        containerId,
                        List.of("cat", path)
                );

        if (result.exitCode() != 0) {
            throw new DockerExecutionException(
                    "Failed to read file [" + path + "] from container ["
                            + containerId + "]: "
                            + result.stderr()
            );
        }

        return result.stdout();
    }

    @Override
    public void writeFile(
            String containerId,
            String path,
            String content
    ) throws DockerExecutionException {

        String encodedContent =
                Base64.getEncoder()
                        .encodeToString(
                                content.getBytes(StandardCharsets.UTF_8)
                        );

        DockerExecutionResult result =
                execContainer(
                        containerId,
                        List.of(
                                "bash",
                                "-c",
                                "mkdir -p \"$(dirname '" + path + "')\" && echo '" + encodedContent
                                        + "' | base64 -d > '" + path + "'"
                        )
                );

        if (result.exitCode() != 0) {
            throw new DockerExecutionException(
                    "Failed to write file [" + path + "] to container ["
                            + containerId + "]: "
                            + result.stderr()
            );
        }
    }


    @Override
    public void deleteContainer(String containerId)
            throws DockerContainerDeletionException {

        requireManagedContainer(containerId);

        try {
            dockerClient
                    .removeContainerCmd(containerId)
                    .withForce(true)
                    .exec();

            log.info("Deleted Docker container [{}].", containerId);

        } catch (NotFoundException e) {
            log.warn(
                    "Docker container [{}] no longer exists. Removing stale registration.",
                    containerId
            );

        } catch (Exception e) {
            log.error(
                    "Failed to delete Docker container [{}].",
                    containerId,
                    e
            );

            throw new DockerContainerDeletionException(
                    "Failed to delete Docker container: " + containerId,
                    e
            );
        }

        // Docker deletion succeeded, or Docker confirmed it was already absent.
        try {
            containerRegistry.unregister(containerId);
        } catch (Exception e) {
            log.error(
                    "Container [{}] is absent from Docker, but its Redis registration could not be removed.",
                    containerId,
                    e
            );

            throw new DockerContainerDeletionException(
                    "Container was removed from Docker, but its registration could not be removed: "
                            + containerId,
                    e
            );
        }
    }

}