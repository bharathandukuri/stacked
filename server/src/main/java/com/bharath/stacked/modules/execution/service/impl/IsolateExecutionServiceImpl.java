package com.bharath.stacked.modules.execution.service.impl;

import com.bharath.stacked.modules.execution.exception.IsolateCleanupException;
import com.bharath.stacked.modules.execution.exception.IsolateExecutionException;
import com.bharath.stacked.modules.execution.exception.IsolateInitializationException;
import com.bharath.stacked.modules.execution.model.*;
import com.bharath.stacked.modules.execution.service.DockerExecutionService;
import com.bharath.stacked.modules.execution.service.IsolateExecutionService;
import com.bharath.stacked.modules.execution.service.IsolateMetadataParserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class IsolateExecutionServiceImpl implements IsolateExecutionService {

    private final DockerExecutionService dockerExecutionService;
    private final IsolateMetadataParserService isolateMetadataParserService;

    private int generateBoxId() {
        return ThreadLocalRandom.current()
                .nextInt(1, 1000);
    }

    private String getBoxDirectory(int boxId) {
        return "/var/lib/isolate/" + boxId + "/box";
    }

    @Override
    public SandBoxDetails initialize(DockerContainerDetails dockerContainer) throws IsolateInitializationException {
        if (dockerContainer == null || dockerContainer.id() == null || dockerContainer.id().isBlank()) {
            throw new IsolateInitializationException("Docker container details must not be null or empty.");
        }

        int isolateBoxId = generateBoxId();

        try {
            DockerExecutionResult result =
                    dockerExecutionService.execContainer(
                            dockerContainer.id(),
                            List.of(
                                    "isolate",
                                    "--init",
                                    "--box-id=" + isolateBoxId
                            )
                    );

            if (result.exitCode() != 0) {
                throw new IsolateInitializationException(
                        "Failed to initialize Isolate box "
                                + isolateBoxId
                                + ": "
                                + result.stderr()
                );
            }

            return new SandBoxDetails(
                    isolateBoxId,
                    dockerContainer
            );

        } catch (IsolateInitializationException e) {
            throw e;

        } catch (Exception e) {
            throw new IsolateInitializationException(
                    "Failed to initialize Isolate sandbox in container [" + dockerContainer.id() + "].",
                    e
            );
        }
    }

    @Override
    public void cleanup(SandBoxDetails sandboxDetails)
            throws IsolateCleanupException {

        if (sandboxDetails == null || sandboxDetails.dockerContainerDetails() == null) {
            return;
        }

        String containerId =
                sandboxDetails.dockerContainerDetails().id();

        int boxId = sandboxDetails.isolateBoxId();

        try {
            log.info(
                    "Cleaning up Isolate sandbox [boxId={}, containerId={}]",
                    boxId,
                    containerId
            );

            DockerExecutionResult result =
                    dockerExecutionService.execContainer(
                            containerId,
                            List.of(
                                    "isolate",
                                    "--cleanup",
                                    "--box-id=" + boxId
                            )
                    );

            if (result.exitCode() != 0) {
                throw new IsolateCleanupException(
                        "Failed to cleanup Isolate box "
                                + boxId
                                + ": "
                                + result.stderr()
                );
            }

        } catch (IsolateCleanupException e) {
            throw e;

        } catch (Exception e) {
            log.error(
                    "Failed to cleanup Isolate sandbox [boxId={}, containerId={}]",
                    boxId,
                    containerId,
                    e
            );

            throw new IsolateCleanupException(
                    "Failed to cleanup Isolate sandbox: " + boxId,
                    e
            );
        }
    }

    private List<String> buildIsolateCommand(
            SandBoxDetails sandboxDetails,
            List<String> command,
            String stdin,
            IsolateExecutionConstraints constraints
    ) {
        int boxId = sandboxDetails.isolateBoxId();
        String boxDir = getBoxDirectory(boxId);

        List<String> isolateCommand = new java.util.ArrayList<>();

        isolateCommand.add("isolate");
        isolateCommand.add("--box-id=" + boxId);

        if (constraints.cpuTimeSeconds() != null) {
            isolateCommand.add("--time=" + constraints.cpuTimeSeconds());
        }
        if (constraints.wallTimeSeconds() != null) {
            isolateCommand.add("--wall-time=" + constraints.wallTimeSeconds());
        }
        if (constraints.memoryKb() != null) {
            isolateCommand.add("--mem=" + constraints.memoryKb());
        }
        if (constraints.processLimit() != null) {
            isolateCommand.add("--processes=" + constraints.processLimit());
        }
        if (constraints.fileSizeKb() != null) {
            isolateCommand.add("--fsize=" + constraints.fileSizeKb());
        }

        isolateCommand.add("--meta=" + boxDir + "/meta.txt");
        isolateCommand.add("--stdin=stdin.txt");
        isolateCommand.add("--stdout=stdout.txt");
        isolateCommand.add("--stderr=stderr.txt");
        isolateCommand.add("--dir=/etc:maybe");
        isolateCommand.add("--full-env");

        isolateCommand.add("--run");
        isolateCommand.add("--");

        isolateCommand.addAll(command);

        return isolateCommand;
    }

    @Override
    public IsolateExecutionResult executeWithConstraints(
            SandBoxDetails sandboxDetails,
            List<String> command,
            String stdin,
            IsolateExecutionConstraints executionConstraints
    ) throws IsolateExecutionException {

        String containerId =
                sandboxDetails.dockerContainerDetails().id();
        int boxId = sandboxDetails.isolateBoxId();
        String boxDir = getBoxDirectory(boxId);

        try {
            dockerExecutionService.writeFile(
                    containerId,
                    boxDir + "/stdin.txt",
                    stdin != null ? stdin : ""
            );

            List<String> isolateCommand =
                    buildIsolateCommand(
                            sandboxDetails,
                            command,
                            stdin,
                            executionConstraints
                    );

            dockerExecutionService.execContainer(
                    containerId,
                    isolateCommand
            );

            String metadata =
                    dockerExecutionService.readFile(
                            containerId,
                            boxDir + "/meta.txt"
                    );

            String stdout =
                    dockerExecutionService.readFile(
                            containerId,
                            boxDir + "/stdout.txt"
                    );

            String stderr =
                    dockerExecutionService.readFile(
                            containerId,
                            boxDir + "/stderr.txt"
                    );

            return isolateMetadataParserService.parseMetadata(
                    metadata,
                    stdout,
                    stderr
            );

        } catch (Exception e) {
            log.error(
                    "Failed to execute command in Isolate sandbox [boxId={}]",
                    sandboxDetails.isolateBoxId(),
                    e
            );

            throw new IsolateExecutionException(
                    "Failed to execute command in Isolate sandbox: "
                            + sandboxDetails.isolateBoxId(),
                    e
            );
        }
    }
}
