package com.example.SpringBackend.controller;

import com.example.SpringBackend.Config.StorageFileNotFoundException;
import com.example.SpringBackend.repository.StorageRepository;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class FileUploadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StorageRepository storageService;

    @InjectMocks
    private FileUploadController fileUploadController;

    @BeforeEach
    void setUp() {
        this.storageService = Mockito.mock(StorageRepository.class);

        this.fileUploadController = new FileUploadController(this.storageService);

        this.mockMvc = MockMvcBuilders.standaloneSetup(fileUploadController)
                .setControllerAdvice(fileUploadController)
                .build();
    }


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
    void shouldServeFile(Resource mockResource) throws Exception {
        Mockito.when(storageService.loadAsResource("test.txt"))
                .thenReturn((Resource) mockResource);

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