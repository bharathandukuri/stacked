package com.bharath.stacked.modules.file.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration(proxyBeanMethods = false)
@EnableScheduling
@EnableConfigurationProperties(FileProperties.class)
@Slf4j
public class FileStorageConfig {

    private final FileProperties fileProperties;

    public FileStorageConfig(@NonNull FileProperties fileProperties) {
        this.fileProperties = fileProperties;
    }

    @PostConstruct
    public void initStorageDirectories() {
        createDirectoryIfNotExists(Paths.get(fileProperties.tempPath()));
        createDirectoryIfNotExists(Paths.get(fileProperties.permanentPath()));
    }

    private void createDirectoryIfNotExists(@NonNull Path path) {
        try {
            if (!Files.exists(path)) {
                Files.createDirectories(path);
                log.info("Initialized file storage directory: {}", path.toAbsolutePath());
            }
        } catch (IOException e) {
            log.error("Could not initialize file storage directory: {}", path.toAbsolutePath(), e);
            throw new IllegalStateException("Failed to initialize file storage directory: " + path, e);
        }
    }
}
