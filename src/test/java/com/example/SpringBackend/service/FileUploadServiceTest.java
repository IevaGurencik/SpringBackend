package com.example.SpringBackend.service;

import java.util.Arrays;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.SpringBackend.Config.StorageFileNotFoundException;
import com.example.SpringBackend.repository.StorageRepository;

@SpringBootTest
@ExtendWith(MockitoExtension.class)
class FileUploadServiceTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private StorageRepository storageService;

    @Test
    void shouldListAllFiles() throws Exception {
        given(this.storageService.loadAllDownloadUrls())
                .willReturn(Arrays.asList("http://localhost/files/first.txt", "http://localhost/files/second.txt"));

        this.mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("files",
                        Matchers.contains("http://localhost/files/first.txt",
                                "http://localhost/files/second.txt")));
    }

    @Test
    public void shouldSaveUploadedFile() throws Exception {
        MockMultipartFile multipartFile = new MockMultipartFile("file", "test.txt",
                "text/plain", "Spring Framework".getBytes());

        this.mvc.perform(multipart("/").file(multipartFile))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/"));

        then(this.storageService).should().store(multipartFile);
    }

    @Test
    public void should404WhenFileNotFound() throws Exception {
        given(this.storageService.loadAsResource("test.txt"))
                .willThrow(StorageFileNotFoundException.class);

        this.mvc.perform(get("/files/test.txt"))
                .andExpect(status().isNotFound());
    }
}
