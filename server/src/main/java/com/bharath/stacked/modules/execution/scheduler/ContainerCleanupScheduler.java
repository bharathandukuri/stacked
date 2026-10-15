package com.bharath.stacked.modules.execution.scheduler;

import com.bharath.stacked.modules.execution.dto.ManagedContainer;
import com.bharath.stacked.modules.execution.registry.ManagedContainerRegistry;
import com.bharath.stacked.modules.execution.service.DockerExecutionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ContainerCleanupScheduler {

    private final ManagedContainerRegistry containerRegistry;
    private final DockerExecutionService dockerExecutionService;

    @Scheduled(
            cron = "0 */5 * * * *",
            zone = "Asia/Kolkata"
    )
    public void cleanupExpiredContainers() {
        Instant cutoff = Instant.now().minus(30, ChronoUnit.MINUTES);

        List<ManagedContainer> expiredContainers =
                containerRegistry.findCreatedBefore(cutoff);

        for (ManagedContainer container : expiredContainers) {
            try {
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