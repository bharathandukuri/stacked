package com.bharath.stacked.modules.execution.service.impl;

import com.bharath.stacked.modules.execution.config.DockerProperties;
import com.bharath.stacked.modules.execution.exception.*;
import com.bharath.stacked.modules.execution.model.DockerContainerDetails;
import com.bharath.stacked.modules.execution.model.DockerExecutionResult;
import com.bharath.stacked.modules.execution.model.DockerImageDetails;
import com.bharath.stacked.modules.execution.registry.DockerImageRegistry;
import com.bharath.stacked.modules.execution.service.DockerExecutionService;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.BuildImageResultCallback;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
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
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DockerExecutionServiceImpl implements DockerExecutionService {

    private final DockerClient dockerClient;
    private final DockerProperties dockerProperties;

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

        try {
            for (DockerImageRegistry registry : DockerImageRegistry.values()) {
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
            }
        } catch (Exception e) {
            log.warn("Docker daemon image validation failed: {}. Continuing startup.", e.getMessage());
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

        String image = dockerImageDetails.reference();

        if (!isImageExists(dockerImageDetails)) {
            throw new DockerContainerCreationException(
                    "Docker image does not exist: " + image
            );
        }

        String containerName =
                "stacked-execution-" + UUID.randomUUID();

        try {
            CreateContainerResponse response = dockerClient
                    .createContainerCmd(image)
                    .withName(containerName)
                    .withTty(true)
                    .withHostConfig(
                            HostConfig.newHostConfig()
                                    .withPrivileged(true)
                    )
                    .exec();

            log.info(
                    "Created Docker container [{}] from image [{}].",
                    containerName,
                    image
            );

            return new DockerContainerDetails(
                    response.getId(),
                    containerName
            );

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
    public void startContainer(String containerId)
            throws DockerContainerStartException {

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

            dockerClient
                    .execStartCmd(exec.getId())
                    .withDetach(false)
                    .exec(callback)
                    .awaitCompletion();

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
                                "echo '" + encodedContent
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

        try {
            dockerClient
                    .removeContainerCmd(containerId)
                    .withForce(true)
                    .exec();

            log.info(
                    "Deleted Docker container [{}].",
                    containerId
            );

        } catch (NotFoundException e) {
            log.warn(
                    "Docker container [{}] does not exist.",
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
    }
}