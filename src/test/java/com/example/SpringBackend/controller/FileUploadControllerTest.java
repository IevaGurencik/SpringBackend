package com.example.SpringBackend.controller;

import com.example.SpringBackend.exception.StorageFileNotFoundException;
import com.example.SpringBackend.model.FileMetadataEntity;
import com.example.SpringBackend.service.FileSystemStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.pattern.PathPatternParser;

import java.util.Collections;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class FileUploadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FileSystemStorageService storageService;

    @InjectMocks
    private FileUploadController fileUploadController;

    @BeforeEach
    void setUp() {
        this.storageService = Mockito.mock(FileSystemStorageService.class);
        this.fileUploadController = new FileUploadController((FileSystemStorageService) this.storageService);

        this.mockMvc = MockMvcBuilders.standaloneSetup(fileUploadController)
                .setControllerAdvice(fileUploadController)
                .setPatternParser(new PathPatternParser())
                .build();
    }

    @Test
    void shouldListAllFiles() throws Exception {
        Mockito.when(storageService.loadAllDownloadUrls())
                .thenReturn(Collections.singletonList("http://localhost:8080/api/files/test.txt"));

        mockMvc.perform(get("/api/files"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("http://localhost:8080/api/files/test.txt"));
    }

    @Test
    void shouldServeFile() throws Exception {
        String fileContent = "content";

        org.springframework.core.io.Resource mockResource =
                new org.springframework.core.io.ByteArrayResource(fileContent.getBytes()) {
                    @Override
                    public String getFilename() {
                        return "test.txt";
                    }
                };

        Mockito.when(storageService.loadAsResource("test.txt"))
                .thenReturn(mockResource);

        mockMvc.perform(get("/api/files/test.txt"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "inline; filename=\"test.txt\""))
                .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM))
                .andExpect(content().bytes(fileContent.getBytes()));
    }

    @Test
    void getFileInfo_InvalidNonNumericId_ReturnsNotFound() throws Exception {
        mockMvc.perform(get("/files/abc")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        Mockito.verifyNoInteractions(storageService);
    }

    @Test
    void shouldHandleFileUpload() throws Exception {
        MockMultipartFile mockFile1 = new MockMultipartFile(
                "files",
                "test1.txt",
                "text/plain",
                "hello world 1".getBytes()
        );
        MockMultipartFile mockFile2 = new MockMultipartFile(
                "files",
                "test2.txt",
                "text/plain",
                "hello world 2".getBytes()
        );

        mockMvc.perform(multipart("/api/files")
                        .file(mockFile1)
                        .file(mockFile2)
                        .param("todoId", "1"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Successfully uploaded 2 files!"))
                .andExpect(jsonPath("$.todoId").value(1));

        Mockito.verify(storageService, Mockito.times(1))
                .store(Mockito.any(MultipartFile[].class), Mockito.eq(1L));
    }

    @Test
    void shouldReturn404WhenFileNotFound() throws Exception {
        Mockito.when(storageService.loadAsResource("missing.txt"))
                .thenThrow(new StorageFileNotFoundException("File not found"));

        mockMvc.perform(get("/api/files/missing.txt"))
                .andExpect(status().isNotFound());
    }
}