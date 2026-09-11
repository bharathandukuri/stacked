package com.bharath.stacked.modules.file.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "files")
public class FileMetadata {

    @Id
    @Nullable
    private String id;

    @NonNull
    private String originalFileName;

    @NonNull
    private String storedFileName;

    @NonNull
    private String contentType;

    private long size;

    @NonNull
    @Indexed
    private FileStorageType storageType;

    @NonNull
    private String storagePath;

    @NonNull
    @Indexed
    private String ownerId;

    @CreatedDate
    @NonNull
    @Builder.Default
    private Instant createdAt = Instant.now();

    @LastModifiedDate
    @NonNull
    @Builder.Default
    private Instant updatedAt = Instant.now();

    @Nullable
    @Indexed
    private Instant expiresAt;
}
