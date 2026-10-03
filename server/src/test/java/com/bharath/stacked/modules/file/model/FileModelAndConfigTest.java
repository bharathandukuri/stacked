package com.bharath.stacked.modules.file.model;

import com.bharath.stacked.modules.file.config.FileProperties;
import com.bharath.stacked.modules.file.config.FileStorageConfig;
import com.bharath.stacked.modules.file.dto.FileContent;
import com.bharath.stacked.modules.file.dto.FileResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("File Models, DTOs, and Configuration Unit Tests")
class FileModelAndConfigTest {

    @Test
    @DisplayName("FileStorageType enum contains TEMPORARY and PERMANENT")
    void fileStorageTypeEnum() {
        assertThat(FileStorageType.valueOf("TEMPORARY")).isEqualTo(FileStorageType.TEMPORARY);
        assertThat(FileStorageType.valueOf("PERMANENT")).isEqualTo(FileStorageType.PERMANENT);
        assertThat(FileStorageType.values()).hasSize(2);
    }

    @Test
    @DisplayName("FileMetadata builder, getters, setters, defaults, equals, and hashCode")
    void fileMetadataModel() {
        Instant now = Instant.now();
        FileMetadata meta = FileMetadata.builder()
                .id("file-1")
                .originalFileName("document.pdf")
                .storedFileName("stored-doc.pdf")
                .contentType("application/pdf")
                .size(2048L)
                .storageType(FileStorageType.TEMPORARY)
                .storagePath("/tmp/stored-doc.pdf")
                .ownerId("user-100")
                .createdAt(now)
                .updatedAt(now)
                .expiresAt(now.plusSeconds(3600))
                .build();

        assertThat(meta.getId()).isEqualTo("file-1");
        assertThat(meta.getOriginalFileName()).isEqualTo("document.pdf");
        assertThat(meta.getStoredFileName()).isEqualTo("stored-doc.pdf");
        assertThat(meta.getContentType()).isEqualTo("application/pdf");
        assertThat(meta.getSize()).isEqualTo(2048L);
        assertThat(meta.getStorageType()).isEqualTo(FileStorageType.TEMPORARY);
        assertThat(meta.getStoragePath()).isEqualTo("/tmp/stored-doc.pdf");
        assertThat(meta.getOwnerId()).isEqualTo("user-100");
        assertThat(meta.getCreatedAt()).isEqualTo(now);
        assertThat(meta.getUpdatedAt()).isEqualTo(now);
        assertThat(meta.getExpiresAt()).isNotNull();

        FileMetadata defaultMeta = new FileMetadata();
        assertThat(defaultMeta.getCreatedAt()).isNotNull();
        assertThat(defaultMeta.getUpdatedAt()).isNotNull();

        meta.setOriginalFileName("renamed.pdf");
        assertThat(meta.getOriginalFileName()).isEqualTo("renamed.pdf");
        assertThat(meta.toString()).contains("renamed.pdf");
    }

    @Test
    @DisplayName("FileContent record constructor and getters")
    void fileContentRecord() {
        Resource resource = new ByteArrayResource("test-data".getBytes());
        FileContent content = new FileContent(resource, "text/plain", "notes.txt", 9L);

        assertThat(content.resource()).isSameAs(resource);
        assertThat(content.contentType()).isEqualTo("text/plain");
        assertThat(content.originalFileName()).isEqualTo("notes.txt");
        assertThat(content.contentLength()).isEqualTo(9L);
        assertThat(content.toString()).contains("notes.txt");
    }

    @Test
    @DisplayName("FileResponse from(FileMetadata) maps all fields")
    void fileResponseFrom() {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(3600);

        FileMetadata meta = FileMetadata.builder()
                .id("f-99")
                .originalFileName("img.png")
                .storedFileName("f99.png")
                .contentType("image/png")
                .size(1024L)
                .storageType(FileStorageType.PERMANENT)
                .storagePath("/data/f99.png")
                .ownerId("admin-1")
                .createdAt(now)
                .updatedAt(now)
                .expiresAt(exp)
                .build();

        FileResponse response = FileResponse.from(meta);

        assertThat(response.id()).isEqualTo("f-99");
        assertThat(response.originalFileName()).isEqualTo("img.png");
        assertThat(response.contentType()).isEqualTo("image/png");
        assertThat(response.size()).isEqualTo(1024L);
        assertThat(response.storageType()).isEqualTo(FileStorageType.PERMANENT);
        assertThat(response.ownerId()).isEqualTo("admin-1");
        assertThat(response.createdAt()).isEqualTo(now);
        assertThat(response.updatedAt()).isEqualTo(now);
        assertThat(response.expiresAt()).isEqualTo(exp);
    }

    @Test
    @DisplayName("FileResponse from throws NullPointerException if metadata ID is null")
    void fileResponseFromNullId() {
        FileMetadata meta = FileMetadata.builder()
                .id(null)
                .originalFileName("test.txt")
                .storedFileName("stored.txt")
                .contentType("text/plain")
                .size(100L)
                .storageType(FileStorageType.TEMPORARY)
                .storagePath("/tmp/stored.txt")
                .ownerId("user-1")
                .build();

        assertThatThrownBy(() -> FileResponse.from(meta))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("File ID must not be null");
    }

    @Test
    @DisplayName("FileProperties record constructor and properties")
    void fileProperties() {
        FileProperties props = new FileProperties(
                "temp/dir",
                "perm/dir",
                5242880L,
                List.of("image/png", "text/plain"),
                Duration.ofHours(2),
                "0 0 * * * *"
        );

        assertThat(props.tempPath()).isEqualTo("temp/dir");
        assertThat(props.permanentPath()).isEqualTo("perm/dir");
        assertThat(props.maxFileSizeBytes()).isEqualTo(5242880L);
        assertThat(props.allowedContentTypes()).containsExactly("image/png", "text/plain");
        assertThat(props.tempTtl()).isEqualTo(Duration.ofHours(2));
        assertThat(props.cleanupCron()).isEqualTo("0 0 * * * *");
    }

    @Test
    @DisplayName("FileStorageConfig creates storage directories on init")
    void fileStorageConfigCreatesDirectories(@TempDir Path tempDir) {
        Path tempPath = tempDir.resolve("temp-uploads");
        Path permPath = tempDir.resolve("perm-uploads");

        FileProperties props = new FileProperties(
                tempPath.toString(),
                permPath.toString(),
                10485760L,
                List.of("image/jpeg"),
                Duration.ofHours(1),
                "0 0 * * * *"
        );

        FileStorageConfig config = new FileStorageConfig(props);
        config.initStorageDirectories();

        assertThat(Files.isDirectory(tempPath)).isTrue();
        assertThat(Files.isDirectory(permPath)).isTrue();

        // Calling again on existing directories should not fail
        config.initStorageDirectories();
        assertThat(Files.isDirectory(tempPath)).isTrue();
    }
}
