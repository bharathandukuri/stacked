package com.bharath.stacked.modules.file.repository;

import com.bharath.stacked.modules.file.model.FileMetadata;
import com.bharath.stacked.modules.file.model.FileStorageType;
import org.jspecify.annotations.NonNull;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface FileMetadataRepository extends MongoRepository<FileMetadata, String> {

    @NonNull
    Optional<FileMetadata> findByIdAndOwnerId(@NonNull String id, @NonNull String ownerId);

    @NonNull
    List<FileMetadata> findByStorageTypeAndExpiresAtBefore(@NonNull FileStorageType storageType,
            @NonNull Instant cutoffTime);

    @NonNull
    List<FileMetadata> findByOwnerId(@NonNull String ownerId);

    boolean existsByIdAndOwnerId(@NonNull String id, @NonNull String ownerId);
}
