package com.bharath.stacked.modules.execution.scheduler;

import com.bharath.stacked.modules.execution.dto.ManagedContainer;
import com.bharath.stacked.modules.execution.exception.DockerContainerDeletionException;
import com.bharath.stacked.modules.execution.registry.ManagedContainerRegistry;
import com.bharath.stacked.modules.execution.service.DockerExecutionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ContainerCleanupScheduler Unit Tests")
class ContainerCleanupSchedulerTest {

    @Mock
    private ManagedContainerRegistry containerRegistry;

    @Mock
    private DockerExecutionService dockerExecutionService;

    @org.mockito.Spy
    private com.bharath.stacked.modules.execution.config.ExecutionProperties executionProperties =
            new com.bharath.stacked.modules.execution.config.ExecutionProperties();

    @InjectMocks
    private ContainerCleanupScheduler scheduler;

    @Test
    @DisplayName("cleanupExpiredContainers deletes all expired containers found before cutoff")
    void cleanupDeletesAllExpiredContainers() {
        ManagedContainer c1 = new ManagedContainer("cid-1", "name-1", "img:1", Instant.now().minusSeconds(3600));
        ManagedContainer c2 = new ManagedContainer("cid-2", "name-2", "img:2", Instant.now().minusSeconds(4000));

        when(containerRegistry.findCreatedBefore(any(Instant.class))).thenReturn(List.of(c1, c2));

        scheduler.cleanupExpiredContainers();

        verify(dockerExecutionService).deleteContainer("cid-1");
        verify(dockerExecutionService).deleteContainer("cid-2");

        ArgumentCaptor<Instant> cutoffCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(containerRegistry).findCreatedBefore(cutoffCaptor.capture());
        assertThat(cutoffCaptor.getValue()).isBefore(Instant.now().minusSeconds(1700));
    }

    @Test
    @DisplayName("cleanupExpiredContainers continues processing when one container deletion fails")
    void cleanupContinuesWhenOneFails() {
        ManagedContainer c1 = new ManagedContainer("cid-fail", "name-fail", "img:1", Instant.now().minusSeconds(3600));
        ManagedContainer c2 = new ManagedContainer("cid-ok", "name-ok", "img:2", Instant.now().minusSeconds(3600));

        when(containerRegistry.findCreatedBefore(any(Instant.class))).thenReturn(List.of(c1, c2));
        doThrow(new DockerContainerDeletionException("Deletion failed", new RuntimeException()))
                .when(dockerExecutionService).deleteContainer("cid-fail");

        scheduler.cleanupExpiredContainers();

        verify(dockerExecutionService).deleteContainer("cid-fail");
        verify(dockerExecutionService).deleteContainer("cid-ok");
    }

    @Test
    @DisplayName("cleanupExpiredContainers does nothing when no expired containers found")
    void cleanupDoesNothingWhenEmpty() {
        when(containerRegistry.findCreatedBefore(any(Instant.class))).thenReturn(List.of());

        scheduler.cleanupExpiredContainers();

        verify(dockerExecutionService, never()).deleteContainer(anyString());
    }
}
