package com.bharath.stacked.modules.file.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileCleanupSchedulerTest {

    @Mock
    private FileService fileService;

    @InjectMocks
    private FileCleanupScheduler scheduler;

    @Test
    @DisplayName("runTemporaryFileCleanup executes fileService cleanup")
    void runTemporaryFileCleanup_Executes() {
        when(fileService.cleanupExpiredTemporaryFiles()).thenReturn(5L);

        scheduler.runTemporaryFileCleanup();

        verify(fileService, times(1)).cleanupExpiredTemporaryFiles();
    }

    @Test
    @DisplayName("runTemporaryFileCleanup handles service exceptions gracefully")
    void runTemporaryFileCleanup_HandlesException() {
        when(fileService.cleanupExpiredTemporaryFiles()).thenThrow(new RuntimeException("Storage unavailable"));

        // Must not bubble exception out to scheduler thread
        scheduler.runTemporaryFileCleanup();

        verify(fileService, times(1)).cleanupExpiredTemporaryFiles();
    }
}
