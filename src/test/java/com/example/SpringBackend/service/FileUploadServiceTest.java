package com.example.SpringBackend.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.mock.web.MockHttpServletRequest;

import com.example.SpringBackend.exception.StorageException;
import com.example.SpringBackend.exception.StorageFileNotFoundException;
import com.example.SpringBackend.config.StorageProperties;
import com.example.SpringBackend.repository.StorageRepository;
import com.example.SpringBackend.repository.ToDoRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class FileUploadServiceTest {

    private FileSystemStorageService storageService;

    @Mock
    private StorageRepository storageRepository;

    @Mock
    private ToDoRepository todoRepository;

    @TempDir
    Path sharedTempDir;

    @BeforeEach
    void setUp() {
        StorageProperties properties = new StorageProperties();
        properties.setLocation(sharedTempDir.toString());

        storageService = new FileSystemStorageService(properties, storageRepository, todoRepository);
        storageService.init();

        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @Test
    void shouldSaveUploadedFile() throws IOException {
        MockMultipartFile multipartFile = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                "Spring Framework".getBytes()
        );

        storageService.store(multipartFile);

        Path uploadedFile = sharedTempDir.resolve("test.txt");
        assertThat(Files.exists(uploadedFile)).isTrue();
        assertThat(Files.readString(uploadedFile)).isEqualTo("Spring Framework");
    }

    @Test
    void shouldThrowExceptionWhenFileIsEmpty() {
        MockMultipartFile emptyFile = new MockMultipartFile("file", "empty.txt", "text/plain", new byte[0]);

        assertThatThrownBy(() -> storageService.store(emptyFile))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("Failed to store empty file");
    }

    @Test
    void shouldThrowExceptionForRelativePathAttack() {
        MockMultipartFile maliciousFile = new MockMultipartFile(
                "file",
                "../malicious.txt",
                "text/plain",
                "attack".getBytes()
        );

        assertThatThrownBy(() -> storageService.store(maliciousFile))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("Cannot store file outside current directory");
    }

    @Test
    void shouldListAllFilesAndUrls() throws IOException {
        Files.writeString(sharedTempDir.resolve("first.txt"), "first");
        Files.writeString(sharedTempDir.resolve("second.txt"), "second");

        List<Path> files = storageService.loadAll().collect(Collectors.toList());
        assertThat(files).containsExactlyInAnyOrder(Path.of("first.txt"), Path.of("second.txt"));

        List<String> urls = storageService.loadAllDownloadUrls();
        assertThat(urls).hasSize(2);
        assertThat(urls.get(0)).contains("/files/first.txt");
        assertThat(urls.get(1)).contains("/files/second.txt");
    }

    @Test
    void shouldThrow404WhenFileNotFound() {
        assertThatThrownBy(() -> storageService.loadAsResource("non-existent.txt"))
                .isInstanceOf(StorageFileNotFoundException.class)
                .hasMessageContaining("Could not read file: non-existent.txt");
    }
}