package com.bharath.stacked.modules.file.service.impl;

import com.bharath.stacked.exception.BadRequestException;
import com.bharath.stacked.exception.ForbiddenException;
import com.bharath.stacked.exception.InternalServerException;
import com.bharath.stacked.exception.ResourceNotFoundException;
import com.bharath.stacked.modules.file.config.FileProperties;
import com.bharath.stacked.modules.file.dto.FileContent;
import com.bharath.stacked.modules.file.dto.FileResponse;
import com.bharath.stacked.modules.file.model.FileMetadata;
import com.bharath.stacked.modules.file.model.FileStorageType;
import com.bharath.stacked.modules.file.repository.FileMetadataRepository;
import com.bharath.stacked.modules.file.service.FileService;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@Slf4j
public class FileServiceImpl implements FileService {

    private final FileProperties fileProperties;
    private final FileMetadataRepository fileMetadataRepository;

    public FileServiceImpl(
            @NonNull FileProperties fileProperties,
            @NonNull FileMetadataRepository fileMetadataRepository) {
        this.fileProperties = fileProperties;
        this.fileMetadataRepository = fileMetadataRepository;
    }

    @Override
    @NonNull
    public FileResponse uploadTemporaryFile(@NonNull MultipartFile file, @NonNull String ownerId) {
        validateFile(file);
        return storeFile(file, ownerId, FileStorageType.TEMPORARY);
    }

    @Override
    @NonNull
    public FileResponse uploadPermanentFile(@NonNull MultipartFile file, @NonNull String ownerId) {
        validateFile(file);
        return storeFile(file, ownerId, FileStorageType.PERMANENT);
    }

    @Override
    @NonNull
    public FileResponse makePermanent(@NonNull String fileId, @NonNull String ownerId) {
        FileMetadata metadata = findFileMetadataById(fileId);

        if (!metadata.getOwnerId().equals(ownerId)) {
            log.warn("Access denied: user [{}] attempted to promote file [{}] owned by [{}]",
                    ownerId, fileId, metadata.getOwnerId());
            throw new ForbiddenException("You do not have permission to modify this file.");
        }

        return promoteToPermanent(metadata);
    }

    @Override
    @NonNull
    public FileResponse moveToPermanent(@NonNull String fileId) {
        FileMetadata metadata = findFileMetadataById(fileId);
        return promoteToPermanent(metadata);
    }

    @Override
    @NonNull
    public FileContent getFileContent(@NonNull String fileId) {
        FileMetadata metadata = findFileMetadataById(fileId);
        Path filePath = Paths.get(metadata.getStoragePath()).toAbsolutePath().normalize();

        if (!Files.exists(filePath) || !Files.isReadable(filePath)) {
            log.warn("Physical file missing or unreadable on disk at [{}] for fileId [{}]", filePath, fileId);
            throw new ResourceNotFoundException("File content is unavailable or has expired.");
        }

        try {
            Resource resource = new UrlResource(filePath.toUri());
            return new FileContent(resource, metadata.getContentType(), metadata.getOriginalFileName(),
                    metadata.getSize());
        } catch (MalformedURLException e) {
            log.error("Failed to load file resource for path [{}]: {}", filePath, e.getMessage());
            throw new InternalServerException("Could not read file resource.");
        }
    }

    @Override
    @NonNull
    public FileContent getFileContent(@NonNull String fileId, @NonNull String authenticatedUserId) {
        return getFileContent(fileId);
    }

    @Override
    @NonNull
    public FileResponse getFileMetadata(@NonNull String fileId, @NonNull String authenticatedUserId) {
        FileMetadata metadata = findFileMetadataById(fileId);
        return FileResponse.from(metadata);
    }

    @Override
    public void deleteFile(@NonNull String fileId, @NonNull String ownerId) {
        FileMetadata metadata = findFileMetadataById(fileId);

        if (!metadata.getOwnerId().equals(ownerId)) {
            log.warn("Security violation: user [{}] attempted to delete file [{}] owned by [{}]",
                    ownerId, fileId, metadata.getOwnerId());
            throw new ForbiddenException(
                    "You do not have permission to delete this file. Only the file owner may delete it.");
        }

        Path filePath = Paths.get(metadata.getStoragePath()).toAbsolutePath().normalize();
        try {
            Files.deleteIfExists(filePath);
            log.debug("Deleted physical file at [{}]", filePath);
        } catch (IOException e) {
            log.warn("Failed to delete physical file at [{}]: {}", filePath, e.getMessage());
        }

        fileMetadataRepository.delete(metadata);
        log.info("File [{}] permanently deleted by owner [{}]", fileId, ownerId);
    }

    @Override
    public long cleanupExpiredTemporaryFiles() {
        Instant now = Instant.now();
        List<FileMetadata> expiredFiles = fileMetadataRepository.findByStorageTypeAndExpiresAtBefore(
                FileStorageType.TEMPORARY, now);

        if (expiredFiles.isEmpty()) {
            return 0;
        }

        long deletedCount = 0;
        for (FileMetadata file : expiredFiles) {
            try {
                Path filePath = Paths.get(file.getStoragePath()).toAbsolutePath().normalize();
                Files.deleteIfExists(filePath);
                fileMetadataRepository.delete(file);
                deletedCount++;
            } catch (Exception ex) {
                log.error("Error cleaning up expired temporary file [{}] at [{}]: {}",
                        file.getId(), file.getStoragePath(), ex.getMessage());
            }
        }

        log.info("Temporary file cleanup completed: removed {} expired files", deletedCount);
        return deletedCount;
    }

    @NonNull
    private FileResponse promoteToPermanent(@NonNull FileMetadata metadata) {
        if (metadata.getStorageType() == FileStorageType.PERMANENT) {
            return FileResponse.from(metadata);
        }

        Path sourcePath = Paths.get(metadata.getStoragePath()).toAbsolutePath().normalize();
        Path permanentDir = Paths.get(fileProperties.permanentPath()).toAbsolutePath().normalize();
        Path targetPath = permanentDir.resolve(metadata.getStoredFileName()).normalize();

        try {
            if (!Files.exists(permanentDir)) {
                Files.createDirectories(permanentDir);
            }

            if (Files.exists(sourcePath)) {
                try {
                    Files.move(sourcePath, targetPath, StandardCopyOption.ATOMIC_MOVE,
                            StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException atomicException) {
                    // Fallback to non-atomic copy and delete if cross-device move
                    Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                    Files.deleteIfExists(sourcePath);
                }
            } else {
                log.warn("Physical temporary file [{}] not found during promotion to permanent", sourcePath);
            }

            metadata.setStorageType(FileStorageType.PERMANENT);
            metadata.setStoragePath(targetPath.toAbsolutePath().toString());
            metadata.setExpiresAt(null);
            metadata.setUpdatedAt(Instant.now());

            FileMetadata saved = fileMetadataRepository.save(metadata);
            log.info("File [{}] successfully moved to permanent storage at [{}]", saved.getId(), targetPath);
            return FileResponse.from(saved);
        } catch (IOException e) {
            log.error("Failed to move file [{}] from [{}] to [{}]: {}",
                    metadata.getId(), sourcePath, targetPath, e.getMessage());
            throw new InternalServerException("Failed to move file to permanent storage.");
        }
    }

    @NonNull
    private FileResponse storeFile(
            @NonNull MultipartFile file,
            @NonNull String ownerId,
            @NonNull FileStorageType storageType) {
        String originalFilename = StringUtils
                .cleanPath(Objects.requireNonNullElse(file.getOriginalFilename(), "unnamed_file"));
        String sanitizedBaseName = sanitizeFilename(originalFilename);
        String fileId = UUID.randomUUID().toString();
        String storedFileName = fileId + "_" + sanitizedBaseName;

        Path storageDir = Paths.get(storageType == FileStorageType.TEMPORARY
                ? fileProperties.tempPath()
                : fileProperties.permanentPath()).toAbsolutePath().normalize();

        Path destinationPath = storageDir.resolve(storedFileName).normalize();

        if (!destinationPath.startsWith(storageDir)) {
            throw new BadRequestException("Invalid path sequence detected in file name.");
        }

        try {
            if (!Files.exists(storageDir)) {
                Files.createDirectories(storageDir);
            }

            Files.copy(file.getInputStream(), destinationPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("Failed to store file [{}] at [{}]: {}", originalFilename, destinationPath, e.getMessage());
            throw new InternalServerException("Could not store uploaded file.");
        }

        Instant now = Instant.now();
        Instant expiresAt = (storageType == FileStorageType.TEMPORARY)
                ? now.plus(fileProperties.tempTtl())
                : null;

        FileMetadata metadata = FileMetadata.builder()
                .id(fileId)
                .originalFileName(originalFilename)
                .storedFileName(storedFileName)
                .contentType(file.getContentType() != null ? file.getContentType() : "application/octet-stream")
                .size(file.getSize())
                .storageType(storageType)
                .storagePath(destinationPath.toAbsolutePath().toString())
                .ownerId(ownerId)
                .createdAt(now)
                .updatedAt(now)
                .expiresAt(expiresAt)
                .build();

        FileMetadata saved = fileMetadataRepository.save(metadata);
        log.info("Stored [{}] file [{}] for owner [{}], size: {} bytes",
                storageType, saved.getId(), ownerId, saved.getSize());

        return FileResponse.from(saved);
    }

    private void validateFile(@NonNull MultipartFile file) {
        if (file.isEmpty()) {
            throw new BadRequestException("Uploaded file cannot be empty.");
        }

        if (file.getSize() > fileProperties.maxFileSizeBytes()) {
            throw new BadRequestException(
                    String.format("File size [%d bytes] exceeds maximum permitted limit [%d bytes].",
                            file.getSize(), fileProperties.maxFileSizeBytes()));
        }

        String rawContentType = file.getContentType();
        if (rawContentType == null || rawContentType.isBlank()) {
            throw new BadRequestException("File content type must be specified.");
        }

        String contentType = rawContentType.toLowerCase().trim();
        boolean isAllowed = fileProperties.allowedContentTypes().stream()
                .map(allowed -> allowed.toLowerCase().trim())
                .anyMatch(allowed -> allowed.equals(contentType) || (allowed.endsWith("/*")
                        && contentType.startsWith(allowed.substring(0, allowed.length() - 1))));

        if (!isAllowed) {
            throw new BadRequestException(String.format("Content type [%s] is not permitted.", rawContentType));
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null && (originalFilename.contains("..") || originalFilename.contains("/")
                || originalFilename.contains("\\") || originalFilename.contains("\0"))) {
            throw new BadRequestException("Filename contains illegal path traversal characters.");
        }
    }

    @NonNull
    private FileMetadata findFileMetadataById(@NonNull String fileId) {
        return fileMetadataRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File with ID [" + fileId + "] was not found."));
    }

    @NonNull
    private String sanitizeFilename(@NonNull String filename) {
        // Keep only alphanumeric characters, underscores, hyphens, and dots
        String clean = filename.replaceAll("[^a-zA-Z0-9._-]", "_");
        if (clean.isBlank() || clean.equals(".")) {
            return "file";
        }
        return clean;
    }
}
