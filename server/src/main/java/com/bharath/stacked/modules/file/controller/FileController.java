package com.bharath.stacked.modules.file.controller;

import com.bharath.stacked.common.api.ApiResponse;
import com.bharath.stacked.exception.UnauthorizedException;
import com.bharath.stacked.modules.file.dto.FileContent;
import com.bharath.stacked.modules.file.dto.FileResponse;
import com.bharath.stacked.modules.file.service.FileService;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileService fileService;

    public FileController(@NonNull FileService fileService) {
        this.fileService = fileService;
    }

    /**
     * Upload a file to temporary storage.
     */
    @PostMapping(value = "/temporary", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @NonNull
    public ResponseEntity<ApiResponse<FileResponse>> uploadTemporary(
            @RequestParam("file") @NonNull MultipartFile file,
            @NonNull Authentication authentication) {
        String userId = getAuthenticatedUserId(authentication);
        FileResponse response = fileService.uploadTemporaryFile(file, userId);
        return ApiResponse.created(response, "File uploaded to temporary storage successfully");
    }

    /**
     * Upload a file directly to permanent storage.
     */
    @PostMapping(value = "/permanent", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @NonNull
    public ResponseEntity<ApiResponse<FileResponse>> uploadPermanent(
            @RequestParam("file") @NonNull MultipartFile file,
            @NonNull Authentication authentication) {
        String userId = getAuthenticatedUserId(authentication);
        FileResponse response = fileService.uploadPermanentFile(file, userId);
        return ApiResponse.created(response, "File uploaded to permanent storage successfully");
    }

    /**
     * Move a temporary file to permanent storage.
     */
    @PostMapping("/{id}/make-permanent")
    @NonNull
    public ResponseEntity<ApiResponse<FileResponse>> makePermanent(
            @PathVariable("id") @NonNull String id,
            @NonNull Authentication authentication) {
        String userId = getAuthenticatedUserId(authentication);
        FileResponse response = fileService.makePermanent(id, userId);
        return ApiResponse.success(response, "File moved to permanent storage successfully");
    }

    /**
     * Retrieve file metadata.
     */
    @GetMapping("/{id}")
    @NonNull
    public ResponseEntity<ApiResponse<FileResponse>> getMetadata(
            @PathVariable("id") @NonNull String id,
            @NonNull Authentication authentication) {
        String userId = getAuthenticatedUserId(authentication);
        FileResponse response = fileService.getFileMetadata(id, userId);
        return ApiResponse.success(response);
    }

    /**
     * Stream or download file contents (public access for embedded browser media).
     */
    @GetMapping("/{id}/content")
    @NonNull
    public ResponseEntity<Resource> getContent(@PathVariable("id") @NonNull String id) {
        FileContent content = fileService.getFileContent(id);

        HttpHeaders headers = new HttpHeaders();
        try {
            headers.setContentType(MediaType.parseMediaType(content.contentType()));
        } catch (Exception e) {
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        }

        headers.setContentLength(content.contentLength());
        headers.setContentDisposition(ContentDisposition.inline()
                .filename(content.originalFileName(), StandardCharsets.UTF_8)
                .build());

        return ResponseEntity.ok()
                .headers(headers)
                .body(content.resource());
    }

    /**
     * Delete a file permanently. Strict ownership required.
     */
    @DeleteMapping("/{id}")
    @NonNull
    public ResponseEntity<ApiResponse<Void>> deleteFile(
            @PathVariable("id") @NonNull String id,
            @NonNull Authentication authentication) {
        String userId = getAuthenticatedUserId(authentication);
        fileService.deleteFile(id, userId);
        return ApiResponse.success(null, "File deleted successfully");
    }

    @NonNull
    private String getAuthenticatedUserId(@Nullable Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("Authentication is required to access file resources.");
        }
        String name = authentication.getName();
        if (name == null || name.isBlank()) {
            throw new UnauthorizedException("Authenticated user identifier is missing.");
        }
        return name;
    }
}
