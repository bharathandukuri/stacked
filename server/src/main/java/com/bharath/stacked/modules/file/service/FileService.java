package com.bharath.stacked.modules.file.service;

import com.bharath.stacked.modules.file.dto.FileContent;
import com.bharath.stacked.modules.file.dto.FileResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.web.multipart.MultipartFile;

public interface FileService {

    /**
     * Upload a file to temporary storage. Automatically expires based on configured
     * TTL.
     */
    @NonNull
    FileResponse uploadTemporaryFile(@NonNull MultipartFile file, @NonNull String ownerId);

    /**
     * Upload a file directly to permanent storage.
     */
    @NonNull
    FileResponse uploadPermanentFile(@NonNull MultipartFile file, @NonNull String ownerId);

    /**
     * Move an existing temporary file to permanent storage, verifying ownership.
     */
    @NonNull
    FileResponse makePermanent(@NonNull String fileId, @NonNull String ownerId);

    /**
     * Move a temporary file to permanent storage for internal module consumers
     * (e.g. Assessment/Question bank).
     */
    @NonNull
    FileResponse moveToPermanent(@NonNull String fileId);

    /**
     * Retrieve file content for streaming or download (public access for browser
     * media).
     */
    @NonNull
    FileContent getFileContent(@NonNull String fileId);

    /**
     * Retrieve file content for streaming or download.
     */
    @NonNull
    FileContent getFileContent(@NonNull String fileId, @NonNull String authenticatedUserId);

    /**
     * Retrieve file metadata.
     */
    @NonNull
    FileResponse getFileMetadata(@NonNull String fileId, @NonNull String authenticatedUserId);

    /**
     * Delete a file permanently. Only the file owner can delete it.
     */
    void deleteFile(@NonNull String fileId, @NonNull String ownerId);

    /**
     * Clean up expired temporary files from disk and database.
     * 
     * @return count of deleted files
     */
    long cleanupExpiredTemporaryFiles();
}
