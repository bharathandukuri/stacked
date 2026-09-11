package com.bharath.stacked.modules.file.controller;

import com.bharath.stacked.exception.GlobalExceptionHandler;
import com.bharath.stacked.modules.file.dto.FileContent;
import com.bharath.stacked.modules.file.dto.FileResponse;
import com.bharath.stacked.modules.file.model.FileStorageType;
import com.bharath.stacked.modules.file.service.FileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class FileControllerTest {

    @Mock
    private FileService fileService;

    @InjectMocks
    private FileController fileController;

    private MockMvc mockMvc;
    private Authentication auth;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(fileController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        auth = new UsernamePasswordAuthenticationToken(
                "user-123",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    @Test
    @DisplayName("POST /api/files/temporary uploads file and returns 201 Created")
    void uploadTemporary_Success() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.png", "image/png", "image-bytes".getBytes());

        FileResponse response = new FileResponse(
                "file-1", "test.png", "image/png", 11L,
                FileStorageType.TEMPORARY, "user-123",
                Instant.now(), Instant.now(), Instant.now().plusSeconds(3600));

        when(fileService.uploadTemporaryFile(any(), eq("user-123"))).thenReturn(response);

        mockMvc.perform(multipart("/api/files/temporary")
                        .file(file)
                        .principal(auth))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("file-1"))
                .andExpect(jsonPath("$.data.storageType").value("TEMPORARY"));
    }

    @Test
    @DisplayName("POST /api/files/permanent uploads permanent file and returns 201 Created")
    void uploadPermanent_Success() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "doc.pdf", "application/pdf", "pdf-bytes".getBytes());

        FileResponse response = new FileResponse(
                "file-2", "doc.pdf", "application/pdf", 9L,
                FileStorageType.PERMANENT, "user-123",
                Instant.now(), Instant.now(), null);

        when(fileService.uploadPermanentFile(any(), eq("user-123"))).thenReturn(response);

        mockMvc.perform(multipart("/api/files/permanent")
                        .file(file)
                        .principal(auth))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("file-2"))
                .andExpect(jsonPath("$.data.storageType").value("PERMANENT"));
    }

    @Test
    @DisplayName("POST /api/files/{id}/make-permanent promotes temporary file")
    void makePermanent_Success() throws Exception {
        FileResponse response = new FileResponse(
                "file-1", "test.png", "image/png", 11L,
                FileStorageType.PERMANENT, "user-123",
                Instant.now(), Instant.now(), null);

        when(fileService.makePermanent("file-1", "user-123")).thenReturn(response);

        mockMvc.perform(post("/api/files/file-1/make-permanent")
                        .principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.storageType").value("PERMANENT"));
    }

    @Test
    @DisplayName("GET /api/files/{id} returns metadata")
    void getMetadata_Success() throws Exception {
        FileResponse response = new FileResponse(
                "file-1", "test.png", "image/png", 11L,
                FileStorageType.PERMANENT, "user-123",
                Instant.now(), Instant.now(), null);

        when(fileService.getFileMetadata("file-1", "user-123")).thenReturn(response);

        mockMvc.perform(get("/api/files/file-1")
                        .principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("file-1"));
    }

    @Test
    @DisplayName("GET /api/files/{id}/content streams file resource")
    void getContent_Success() throws Exception {
        byte[] contentBytes = "file-binary-stream-data".getBytes();
        ByteArrayResource resource = new ByteArrayResource(contentBytes);
        FileContent fileContent = new FileContent(resource, "text/plain", "notes.txt", contentBytes.length);

        when(fileService.getFileContent("file-1", "user-123")).thenReturn(fileContent);

        mockMvc.perform(get("/api/files/file-1/content")
                        .principal(auth))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/plain"))
                .andExpect(header().string("Content-Length", String.valueOf(contentBytes.length)))
                .andExpect(content().bytes(contentBytes));
    }

    @Test
    @DisplayName("DELETE /api/files/{id} calls fileService and returns success")
    void deleteFile_Success() throws Exception {
        doNothing().when(fileService).deleteFile("file-1", "user-123");

        mockMvc.perform(delete("/api/files/file-1")
                        .principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(fileService, times(1)).deleteFile("file-1", "user-123");
    }
}
