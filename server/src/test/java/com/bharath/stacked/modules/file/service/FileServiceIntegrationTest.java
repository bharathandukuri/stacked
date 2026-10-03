package com.bharath.stacked.modules.file.service;

import com.bharath.stacked.AbstractIntegrationTest;
import com.bharath.stacked.exception.ForbiddenException;
import com.bharath.stacked.modules.file.config.FileProperties;
import com.bharath.stacked.modules.file.dto.FileContent;
import com.bharath.stacked.modules.file.dto.FileResponse;
import com.bharath.stacked.modules.file.model.FileMetadata;
import com.bharath.stacked.modules.file.model.FileStorageType;
import com.bharath.stacked.modules.file.repository.FileMetadataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("FileService Live Integration Tests")
class FileServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private FileService fileService;

    @Autowired
    private FileMetadataRepository fileMetadataRepository;

    @Autowired
    private FileProperties fileProperties;

    @BeforeEach
    void setUp() {
        fileMetadataRepository.deleteAll();
    }

    @Test
    @DisplayName("Upload temporary file writes to disk and persists metadata in MongoDB")
    void uploadTemporaryFileLive() throws IOException {
        byte[] fileBytes = "integration-test-temp-data".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile multipartFile = new MockMultipartFile(
                "file",
                "test-document.pdf",
                "application/pdf",
                fileBytes
        );

        FileResponse response = fileService.uploadTemporaryFile(multipartFile, "owner-001");

        assertThat(response).isNotNull();
        assertThat(response.originalFileName()).isEqualTo("test-document.pdf");
        assertThat(response.contentType()).isEqualTo("application/pdf");
        assertThat(response.storageType()).isEqualTo(FileStorageType.TEMPORARY);
        assertThat(response.ownerId()).isEqualTo("owner-001");
        assertThat(response.size()).isEqualTo(fileBytes.length);
        assertThat(response.expiresAt()).isNotNull();

        // Verify MongoDB document
        Optional<FileMetadata> savedMeta = fileMetadataRepository.findById(response.id());
        assertThat(savedMeta).isPresent();
        FileMetadata meta = savedMeta.get();
        assertThat(meta.getStorageType()).isEqualTo(FileStorageType.TEMPORARY);

        // Verify physical file on disk
        Path physicalPath = Paths.get(meta.getStoragePath());
        assertThat(Files.exists(physicalPath)).isTrue();
        assertThat(Files.readAllBytes(physicalPath)).isEqualTo(fileBytes);

        // Clean up physical file
        Files.deleteIfExists(physicalPath);
    }

    @Test
    @DisplayName("Upload permanent file stores without expiration")
    void uploadPermanentFileLive() throws IOException {
        byte[] fileBytes = "permanent-asset-data".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile multipartFile = new MockMultipartFile(
                "file",
                "logo.png",
                "image/png",
                fileBytes
        );

        FileResponse response = fileService.uploadPermanentFile(multipartFile, "owner-002");

        assertThat(response.storageType()).isEqualTo(FileStorageType.PERMANENT);
        assertThat(response.expiresAt()).isNull();

        Optional<FileMetadata> savedMeta = fileMetadataRepository.findById(response.id());
        assertThat(savedMeta).isPresent();
        Path physicalPath = Paths.get(savedMeta.get().getStoragePath());
        assertThat(Files.exists(physicalPath)).isTrue();

        Files.deleteIfExists(physicalPath);
    }

    @Test
    @DisplayName("Make permanent promotes file and moves from temp to permanent folder")
    void makePermanentPromotionLive() throws IOException {
        MockMultipartFile multipartFile = new MockMultipartFile(
                "file",
                "sample.txt",
                "text/plain",
                "promotable content".getBytes(StandardCharsets.UTF_8)
        );

        FileResponse tempResponse = fileService.uploadTemporaryFile(multipartFile, "owner-promote");
        Path tempPath = Paths.get(fileMetadataRepository.findById(tempResponse.id()).orElseThrow().getStoragePath());
        assertThat(Files.exists(tempPath)).isTrue();

        FileResponse permResponse = fileService.makePermanent(tempResponse.id(), "owner-promote");

        assertThat(permResponse.storageType()).isEqualTo(FileStorageType.PERMANENT);
        assertThat(permResponse.expiresAt()).isNull();

        // Old temp path must no longer exist (moved)
        assertThat(Files.exists(tempPath)).isFalse();

        // New perm path must exist
        Path newPermPath = Paths.get(fileMetadataRepository.findById(tempResponse.id()).orElseThrow().getStoragePath());
        assertThat(Files.exists(newPermPath)).isTrue();

        Files.deleteIfExists(newPermPath);
    }

    @Test
    @DisplayName("Make permanent throws ForbiddenException when caller is not the file owner")
    void makePermanentForbiddenLive() throws IOException {
        MockMultipartFile multipartFile = new MockMultipartFile(
                "file",
                "secure.pdf",
                "application/pdf",
                "confidential".getBytes(StandardCharsets.UTF_8)
        );

        FileResponse response = fileService.uploadTemporaryFile(multipartFile, "original-owner");
        Path tempPath = Paths.get(fileMetadataRepository.findById(response.id()).orElseThrow().getStoragePath());

        assertThatThrownBy(() -> fileService.makePermanent(response.id(), "hacker-user"))
                .isInstanceOf(ForbiddenException.class);

        Files.deleteIfExists(tempPath);
    }

    @Test
    @DisplayName("Get file content streams exact bytes from disk matching metadata")
    void getFileContentLive() throws IOException {
        byte[] expectedBytes = "streamable-bytes-test".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile multipartFile = new MockMultipartFile(
                "file",
                "stream.txt",
                "text/plain",
                expectedBytes
        );

        FileResponse response = fileService.uploadPermanentFile(multipartFile, "owner-stream");

        FileContent content = fileService.getFileContent(response.id(), "owner-stream");
        assertThat(content).isNotNull();
        assertThat(content.contentType()).isEqualTo("text/plain");
        assertThat(content.originalFileName()).isEqualTo("stream.txt");
        assertThat(content.contentLength()).isEqualTo(expectedBytes.length);
        assertThat(content.resource().getInputStream().readAllBytes()).isEqualTo(expectedBytes);

        Path physicalPath = Paths.get(fileMetadataRepository.findById(response.id()).orElseThrow().getStoragePath());
        Files.deleteIfExists(physicalPath);
    }

    @Test
    @DisplayName("Delete file removes physical file from disk and deletes metadata from MongoDB")
    void deleteFileLive() throws IOException {
        MockMultipartFile multipartFile = new MockMultipartFile(
                "file",
                "delete-target.txt",
                "text/plain",
                "content-to-destroy".getBytes(StandardCharsets.UTF_8)
        );

        FileResponse response = fileService.uploadPermanentFile(multipartFile, "owner-delete");
        Path physicalPath = Paths.get(fileMetadataRepository.findById(response.id()).orElseThrow().getStoragePath());
        assertThat(Files.exists(physicalPath)).isTrue();

        fileService.deleteFile(response.id(), "owner-delete");

        assertThat(Files.exists(physicalPath)).isFalse();
        assertThat(fileMetadataRepository.findById(response.id())).isEmpty();
    }

    @Test
    @DisplayName("Cleanup expired temporary files purges stale files from disk and MongoDB")
    void cleanupExpiredTemporaryFilesLive() throws IOException {
        Path tempDir = Paths.get(fileProperties.tempPath()).toAbsolutePath().normalize();
        Files.createDirectories(tempDir);

        Path expiredDiskPath = tempDir.resolve("expired-file-123.txt");
        Files.writeString(expiredDiskPath, "expired content");

        FileMetadata expiredMeta = FileMetadata.builder()
                .id("expired-id-123")
                .originalFileName("old.txt")
                .storedFileName("expired-file-123.txt")
                .contentType("text/plain")
                .size(15L)
                .storageType(FileStorageType.TEMPORARY)
                .storagePath(expiredDiskPath.toString())
                .ownerId("user-cleanup")
                .createdAt(Instant.now().minusSeconds(7200))
                .updatedAt(Instant.now().minusSeconds(7200))
                .expiresAt(Instant.now().minusSeconds(3600))
                .build();
        fileMetadataRepository.save(expiredMeta);

        long cleanedCount = fileService.cleanupExpiredTemporaryFiles();

        assertThat(cleanedCount).isGreaterThanOrEqualTo(1L);
        assertThat(Files.exists(expiredDiskPath)).isFalse();
        assertThat(fileMetadataRepository.findById("expired-id-123")).isEmpty();
    }
}
