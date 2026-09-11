package com.bharath.stacked.modules.file.dto;

import com.bharath.stacked.modules.file.model.FileMetadata;
import com.bharath.stacked.modules.file.model.FileStorageType;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.Objects;

public record FileResponse(
        @NonNull String id,
        @NonNull String originalFileName,
        @NonNull String contentType,
        long size,
        @NonNull FileStorageType storageType,
        @NonNull String ownerId,
        @NonNull Instant createdAt,
        @NonNull Instant updatedAt,
        @Nullable Instant expiresAt) {

    @NonNull
    public static FileResponse from(@NonNull FileMetadata metadata) {
        return new FileResponse(
                Objects.requireNonNull(metadata.getId(), "File ID must not be null"),
                metadata.getOriginalFileName(),
                metadata.getContentType(),
                metadata.getSize(),
                metadata.getStorageType(),
                metadata.getOwnerId(),
                metadata.getCreatedAt(),
                metadata.getUpdatedAt(),
                metadata.getExpiresAt());
    }
}
