package com.bharath.stacked.modules.execution.service.impl;

import com.bharath.stacked.modules.execution.model.DockerContainerDetails;
import com.bharath.stacked.modules.execution.config.DockerProperties;
import com.bharath.stacked.modules.execution.exception.DockerContainerCreationException;
import com.bharath.stacked.modules.execution.exception.DockerContainerDeletionException;
import com.bharath.stacked.modules.execution.exception.DockerImageCreationException;
import com.bharath.stacked.modules.execution.model.DockerImageDetails;
import com.bharath.stacked.modules.execution.registry.DockerImageRegistry;
import com.bharath.stacked.modules.execution.service.DockerExecutionService;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.BuildImageResultCallback;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.BuildResponseItem;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class DockerExecutionServiceImpl implements DockerExecutionService {

    private final DockerClient dockerClient;
    private final DockerProperties dockerProperties;

    @PostConstruct
    void validateImages() {
        if (!dockerProperties.isEnabled()) {
            log.info("Docker execution is disabled; skipping image validation.");
            return;
        }

        try {
            for (DockerImageRegistry registry : DockerImageRegistry.values()) {
                DockerImageDetails image = registry.dockerImage();

                if (checkImageExists(image)) {
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
    public boolean checkImageExists(DockerImageDetails dockerImageDetails) {
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

            if (!checkImageExists(dockerImageDetails)) {
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

        if (!checkImageExists(dockerImageDetails)) {
            throw new DockerContainerCreationException(
                    "Docker image does not exist: " + image
            );
        }

        try {
            CreateContainerResponse response = dockerClient
                    .createContainerCmd(image)
                    .exec();

            log.info(
                    "Created Docker container [{}] from image [{}].",
                    response.getId(),
                    image
            );

            return new DockerContainerDetails(
                    response.getId(),
                    response.getId()
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