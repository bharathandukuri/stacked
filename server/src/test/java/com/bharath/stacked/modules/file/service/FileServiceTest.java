package com.bharath.stacked.modules.file.service;

import com.bharath.stacked.exception.BadRequestException;
import com.bharath.stacked.exception.ForbiddenException;
import com.bharath.stacked.exception.ResourceNotFoundException;
import com.bharath.stacked.modules.file.config.FileProperties;
import com.bharath.stacked.modules.file.dto.FileContent;
import com.bharath.stacked.modules.file.dto.FileResponse;
import com.bharath.stacked.modules.file.model.FileMetadata;
import com.bharath.stacked.modules.file.model.FileStorageType;
import com.bharath.stacked.modules.file.repository.FileMetadataRepository;
import com.bharath.stacked.modules.file.service.impl.FileServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileServiceTest {

    @Mock
    private FileMetadataRepository fileMetadataRepository;

    @TempDir
    Path tempFolder;

    private Path tempDirPath;
    private Path permDirPath;
    private FileProperties fileProperties;
    private FileServiceImpl fileService;

    @BeforeEach
    void setUp() throws IOException {
        tempDirPath = tempFolder.resolve("temp");
        permDirPath = tempFolder.resolve("perm");
        Files.createDirectories(tempDirPath);
        Files.createDirectories(permDirPath);

        fileProperties = new FileProperties(
                tempDirPath.toString(),
                permDirPath.toString(),
                1048576L, // 1MB for tests
                List.of("image/png", "application/pdf", "text/plain"),
                Duration.ofHours(1),
                "0 0 * * * *");

        fileService = new FileServiceImpl(fileProperties, fileMetadataRepository);
    }

    @Test
    @DisplayName("uploadTemporaryFile stores file in temp directory with expiresAt")
    void uploadTemporaryFile_Success() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar.png", "image/png", "test-image-content".getBytes());

        when(fileMetadataRepository.save(any(FileMetadata.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        FileResponse response = fileService.uploadTemporaryFile(file, "user-123");

        assertNotNull(response);
        assertEquals("avatar.png", response.originalFileName());
        assertEquals("image/png", response.contentType());
        assertEquals(FileStorageType.TEMPORARY, response.storageType());
        assertEquals("user-123", response.ownerId());
        assertNotNull(response.expiresAt());
        verify(fileMetadataRepository, times(1)).save(any(FileMetadata.class));
    }

    @Test
    @DisplayName("uploadPermanentFile stores file in permanent directory without expiresAt")
    void uploadPermanentFile_Success() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "document.pdf", "application/pdf", "test-pdf-content".getBytes());

        when(fileMetadataRepository.save(any(FileMetadata.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        FileResponse response = fileService.uploadPermanentFile(file, "user-123");

        assertNotNull(response);
        assertEquals("document.pdf", response.originalFileName());
        assertEquals(FileStorageType.PERMANENT, response.storageType());
        assertNull(response.expiresAt());
        verify(fileMetadataRepository, times(1)).save(any(FileMetadata.class));
    }

    @Test
    @DisplayName("makePermanent moves file to permanent storage and updates metadata")
    void makePermanent_Success() throws IOException {
        Path sourceFile = tempDirPath.resolve("temp_file.png");
        Files.writeString(sourceFile, "dummy content");

        FileMetadata metadata = FileMetadata.builder()
                .id("file-100")
                .originalFileName("image.png")
                .storedFileName("temp_file.png")
                .contentType("image/png")
                .size(13L)
                .storageType(FileStorageType.TEMPORARY)
                .storagePath(sourceFile.toString())
                .ownerId("user-123")
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        when(fileMetadataRepository.findById("file-100")).thenReturn(Optional.of(metadata));
        when(fileMetadataRepository.save(any(FileMetadata.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        FileResponse response = fileService.makePermanent("file-100", "user-123");

        assertNotNull(response);
        assertEquals(FileStorageType.PERMANENT, response.storageType());
        assertNull(response.expiresAt());
        assertFalse(Files.exists(sourceFile));
        assertTrue(Files.exists(permDirPath.resolve("temp_file.png")));
    }

    @Test
    @DisplayName("makePermanent throws ForbiddenException when caller is not owner")
    void makePermanent_ForbiddenForDifferentUser() {
        FileMetadata metadata = FileMetadata.builder()
                .id("file-100")
                .originalFileName("image.png")
                .storedFileName("temp_file.png")
                .contentType("image/png")
                .storageType(FileStorageType.TEMPORARY)
                .storagePath(tempDirPath.resolve("temp_file.png").toString())
                .ownerId("user-123")
                .build();

        when(fileMetadataRepository.findById("file-100")).thenReturn(Optional.of(metadata));

        assertThrows(ForbiddenException.class, () -> fileService.makePermanent("file-100", "attacker-456"));
    }

    @Test
    @DisplayName("makePermanent throws ResourceNotFoundException when file not found")
    void makePermanent_NotFound() {
        when(fileMetadataRepository.findById("unknown")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> fileService.makePermanent("unknown", "user-123"));
    }

    @Test
    @DisplayName("moveToPermanent promotes file for internal module consumers")
    void moveToPermanent_Success() throws IOException {
        Path sourceFile = tempDirPath.resolve("temp_module_file.pdf");
        Files.writeString(sourceFile, "module pdf content");

        FileMetadata metadata = FileMetadata.builder()
                .id("file-200")
                .originalFileName("module.pdf")
                .storedFileName("temp_module_file.pdf")
                .contentType("application/pdf")
                .size(18L)
                .storageType(FileStorageType.TEMPORARY)
                .storagePath(sourceFile.toString())
                .ownerId("user-123")
                .build();

        when(fileMetadataRepository.findById("file-200")).thenReturn(Optional.of(metadata));
        when(fileMetadataRepository.save(any(FileMetadata.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        FileResponse response = fileService.moveToPermanent("file-200");

        assertEquals(FileStorageType.PERMANENT, response.storageType());
        assertNull(response.expiresAt());
        assertTrue(Files.exists(permDirPath.resolve("temp_module_file.pdf")));
    }

    @Test
    @DisplayName("getFileContent retrieves readable resource with original file metadata")
    void getFileContent_Success() throws IOException {
        Path filePath = permDirPath.resolve("read_test.txt");
        Files.writeString(filePath, "Hello World Content");

        FileMetadata metadata = FileMetadata.builder()
                .id("file-300")
                .originalFileName("hello.txt")
                .storedFileName("read_test.txt")
                .contentType("text/plain")
                .size(19L)
                .storageType(FileStorageType.PERMANENT)
                .storagePath(filePath.toString())
                .ownerId("user-123")
                .build();

        when(fileMetadataRepository.findById("file-300")).thenReturn(Optional.of(metadata));

        FileContent content = fileService.getFileContent("file-300", "user-123");

        assertNotNull(content);
        assertEquals("text/plain", content.contentType());
        assertEquals("hello.txt", content.originalFileName());
        assertTrue(content.resource().exists());
    }

    @Test
    @DisplayName("deleteFile removes physical file and database record for owner")
    void deleteFile_Success() throws IOException {
        Path filePath = permDirPath.resolve("delete_me.txt");
        Files.writeString(filePath, "content to delete");

        FileMetadata metadata = FileMetadata.builder()
                .id("file-400")
                .originalFileName("delete_me.txt")
                .storedFileName("delete_me.txt")
                .contentType("text/plain")
                .storageType(FileStorageType.PERMANENT)
                .storagePath(filePath.toString())
                .ownerId("owner-1")
                .build();

        when(fileMetadataRepository.findById("file-400")).thenReturn(Optional.of(metadata));

        fileService.deleteFile("file-400", "owner-1");

        assertFalse(Files.exists(filePath));
        verify(fileMetadataRepository, times(1)).delete(metadata);
    }

    @Test
    @DisplayName("deleteFile throws ForbiddenException if non-owner attempts deletion")
    void deleteFile_ForbiddenForNonOwner() {
        FileMetadata metadata = FileMetadata.builder()
                .id("file-400")
                .originalFileName("delete_me.txt")
                .storedFileName("delete_me.txt")
                .contentType("text/plain")
                .storageType(FileStorageType.PERMANENT)
                .storagePath(permDirPath.resolve("delete_me.txt").toString())
                .ownerId("owner-1")
                .build();

        when(fileMetadataRepository.findById("file-400")).thenReturn(Optional.of(metadata));

        assertThrows(ForbiddenException.class, () -> fileService.deleteFile("file-400", "intruder-2"));
        verify(fileMetadataRepository, never()).delete(any());
    }

    @Test
    @DisplayName("cleanupExpiredTemporaryFiles deletes physical files and database records")
    void cleanupExpiredTemporaryFiles_Success() throws IOException {
        Path expiredFile1 = tempDirPath.resolve("expired_1.png");
        Path expiredFile2 = tempDirPath.resolve("expired_2.png");
        Files.writeString(expiredFile1, "content1");
        Files.writeString(expiredFile2, "content2");

        FileMetadata m1 = FileMetadata.builder()
                .id("f1")
                .originalFileName("expired_1.png")
                .storedFileName("expired_1.png")
                .contentType("image/png")
                .storagePath(expiredFile1.toString())
                .storageType(FileStorageType.TEMPORARY)
                .ownerId("user-1")
                .build();
        FileMetadata m2 = FileMetadata.builder()
                .id("f2")
                .originalFileName("expired_2.png")
                .storedFileName("expired_2.png")
                .contentType("image/png")
                .storagePath(expiredFile2.toString())
                .storageType(FileStorageType.TEMPORARY)
                .ownerId("user-2")
                .build();

        when(fileMetadataRepository.findByStorageTypeAndExpiresAtBefore(eq(FileStorageType.TEMPORARY),
                any(Instant.class)))
                .thenReturn(List.of(m1, m2));

        long count = fileService.cleanupExpiredTemporaryFiles();

        assertEquals(2, count);
        assertFalse(Files.exists(expiredFile1));
        assertFalse(Files.exists(expiredFile2));
        verify(fileMetadataRepository, times(1)).delete(m1);
        verify(fileMetadataRepository, times(1)).delete(m2);
    }

    @Test
    @DisplayName("Validation rejects empty file")
    void validateFile_Empty() {
        MockMultipartFile emptyFile = new MockMultipartFile("file", "test.png", "image/png", new byte[0]);
        assertThrows(BadRequestException.class, () -> fileService.uploadTemporaryFile(emptyFile, "user-1"));
    }

    @Test
    @DisplayName("Validation rejects unsupported content type")
    void validateFile_UnsupportedContentType() {
        MockMultipartFile invalidType = new MockMultipartFile("file", "test.exe", "application/x-msdownload",
                "binary".getBytes());
        assertThrows(BadRequestException.class, () -> fileService.uploadTemporaryFile(invalidType, "user-1"));
    }

    @Test
    @DisplayName("Validation rejects path traversal filename")
    void validateFile_PathTraversal() {
        MockMultipartFile traversalFile = new MockMultipartFile("file", "../../etc/passwd", "text/plain",
                "data".getBytes());
        assertThrows(BadRequestException.class, () -> fileService.uploadTemporaryFile(traversalFile, "user-1"));
    }
}
