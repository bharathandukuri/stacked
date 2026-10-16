package com.bharath.stacked.modules.execution.scheduler;

import com.bharath.stacked.modules.execution.config.ExecutionProperties;
import com.bharath.stacked.modules.execution.dto.ManagedContainer;
import com.bharath.stacked.modules.execution.registry.ManagedContainerRegistry;
import com.bharath.stacked.modules.execution.service.DockerExecutionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Component
public class ContainerCleanupScheduler {

    private final ManagedContainerRegistry containerRegistry;
    private final DockerExecutionService dockerExecutionService;
    private final ExecutionProperties executionProperties;

    @Autowired
    public ContainerCleanupScheduler(
            ManagedContainerRegistry containerRegistry,
            DockerExecutionService dockerExecutionService,
            ExecutionProperties executionProperties) {
        this.containerRegistry = containerRegistry;
        this.dockerExecutionService = dockerExecutionService;
        this.executionProperties = executionProperties;
    }

    public ContainerCleanupScheduler(
            ManagedContainerRegistry containerRegistry,
            DockerExecutionService dockerExecutionService) {
        this(containerRegistry, dockerExecutionService, new ExecutionProperties());
    }

    @Scheduled(
            cron = "${stacked.execution.cleanup.cron:0 */5 * * * *}",
            zone = "Asia/Kolkata"
    )
    public void cleanupExpiredContainers() {
        long retentionMinutes = executionProperties.getCleanup().getStaleRetentionMinutes();
        Instant cutoff = Instant.now().minus(retentionMinutes, ChronoUnit.MINUTES);

        List<ManagedContainer> expiredContainers =
                containerRegistry.findCreatedBefore(cutoff);

        for (ManagedContainer container : expiredContainers) {
            try {
                if (dockerExecutionService.isContainerRunning(container.containerId())) {
                    try {
                        dockerExecutionService.stopContainer(container.containerId());
                    } catch (Exception stopEx) {
                        log.warn("Failed to stop expired running container [{}]: {}", container.containerId(), stopEx.getMessage());
                    }
                }

                dockerExecutionService.deleteContainer(
                        container.containerId()
                );

                log.info(
                        "Cleaned up expired container [{}], created at [{}].",
                        container.containerId(),
                        container.createdAt()
                );
            } catch (Exception e) {
                log.error(
                        "Failed to clean up expired container [{}].",
                        container.containerId(),
                        e
                );
            }
        }
    }
}