package com.bharath.stacked.modules.file.service;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class FileCleanupScheduler {

    private final FileService fileService;

    public FileCleanupScheduler(@NonNull FileService fileService) {
        this.fileService = fileService;
    }

    /**
     * Periodically cleans up expired temporary files.
     * Default schedule runs at the beginning of every hour (0 0 * * * *).
     */
    @Scheduled(cron = "${file.storage.cleanup-cron:0 0 * * * *}")
    public void runTemporaryFileCleanup() {
        log.info("Starting scheduled temporary file cleanup job...");
        try {
            long deletedCount = fileService.cleanupExpiredTemporaryFiles();
            log.info("Scheduled temporary file cleanup job completed. Removed {} expired files.", deletedCount);
        } catch (Exception e) {
            log.error("Error occurred during scheduled temporary file cleanup: {}", e.getMessage(), e);
        }
    }
}
