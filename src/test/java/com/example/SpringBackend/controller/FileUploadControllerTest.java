package com.example.SpringBackend.controller;

import com.example.SpringBackend.Config.StorageFileNotFoundException;
import com.example.SpringBackend.repository.StorageRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class FileUploadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Mock
    private StorageRepository storageService;

    @Test
    void shouldListAllFiles() throws Exception {
        Mockito.when(storageService.loadAllDownloadUrls())
                .thenReturn(Collections.singletonList("http://localhost:8080/files/test.txt"));

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("uploadForm"))
                .andExpect(model().attributeExists("files"));
    }

    @Test
    void shouldServeFile() throws Exception {
        Resource mockResource = new ByteArrayResource("content".getBytes()) {
            @Override
            public String getFilename() {
                return "test.txt";
            }
        };

        Mockito.when(storageService.loadAsResource("test.txt"))
                .thenReturn((jakarta.annotation.Resource) mockResource);

        mockMvc.perform(get("/files/test.txt"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"test.txt\""))
                .andExpect(content().bytes("content".getBytes()));
    }

    @Test
    void shouldHandleFileUpload() throws Exception {
        MockMultipartFile mockFile = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                "hello world".getBytes()
        );

        mockMvc.perform(multipart("/").file(mockFile))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(flash().attribute("message", "You successfully uploaded test.txt!"));

        Mockito.verify(storageService, Mockito.times(1)).store(mockFile);
    }

    @Test
    void shouldReturn404WhenFileNotFound() throws Exception {
        Mockito.when(storageService.loadAsResource("missing.txt"))
                .thenThrow(new StorageFileNotFoundException("File not found"));

        mockMvc.perform(get("/files/missing.txt"))
                .andExpect(status().isNotFound());
    }
}