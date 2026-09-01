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

    @Test
    void shouldSaveMultipleFilesAndAssignToTodo() throws IOException {
        com.example.SpringBackend.model.ToDoEntity mockTodo = new com.example.SpringBackend.model.ToDoEntity();
        org.mockito.Mockito.when(todoRepository.findById(1L)).thenReturn(java.util.Optional.of(mockTodo));

        MockMultipartFile file1 = new MockMultipartFile("files", "doc1.pdf", "application/pdf", "pdf data".getBytes());
        MockMultipartFile file2 = new MockMultipartFile("files", "img2.png", "image/png", "png data".getBytes());
        org.springframework.web.multipart.MultipartFile[] filesArray = new org.springframework.web.multipart.MultipartFile[]{file1, file2};

        storageService.store(filesArray, 1L);

        org.mockito.Mockito.verify(storageRepository, org.mockito.Mockito.times(2))
                .save(org.mockito.Mockito.any(com.example.SpringBackend.model.FileMetadataEntity.class));

        long createdFilesCount = Files.walk(sharedTempDir, 1)
                .filter(path -> !path.equals(sharedTempDir))
                .count();
        assertThat(createdFilesCount).isEqualTo(2);
    }

    @Test
    void shouldThrowExceptionWhenTodoNotFoundOnUpload() {
        org.mockito.Mockito.when(todoRepository.findById(99L)).thenReturn(java.util.Optional.empty());
        org.springframework.web.multipart.MultipartFile[] emptyArray = new org.springframework.web.multipart.MultipartFile[]{
                new MockMultipartFile("files", "test.txt", "text/plain", "data".getBytes())
        };

        assertThatThrownBy(() -> storageService.store(emptyArray, 99L))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("Cannot upload files. ToDo not found with id: 99");

        org.mockito.Mockito.verify(storageRepository, org.mockito.Mockito.never()).save(org.mockito.Mockito.any());
    }

    @Test
    void shouldLoadResponseByMetadataId_Success() throws IOException {
        Long metadataId = 1L;
        String fakeUuidName = "generated-uuid-name.txt";

        Files.writeString(sharedTempDir.resolve(fakeUuidName), "file content inside uuid");

        com.example.SpringBackend.model.FileMetadataEntity metadata = new com.example.SpringBackend.model.FileMetadataEntity();
        metadata.setFilename("user-original-name.txt");
        metadata.setStoredFilename(fakeUuidName);

        org.mockito.Mockito.when(storageRepository.findById(metadataId)).thenReturn(java.util.Optional.of(metadata));

        java.util.Map<String, Object> response = storageService.loadResponseByMetadataId(metadataId);

        assertThat(response).isNotNull();
        assertThat(response.get("filename")).isEqualTo("user-original-name.txt");
        assertThat(response.get("contentType")).isEqualTo("text/plain");

        org.springframework.core.io.Resource resource = (org.springframework.core.io.Resource) response.get("resource");
        assertThat(resource.exists()).isTrue();
        assertThat(resource.isReadable()).isTrue();
    }

    @Test
    void shouldThrow404WhenMetadataIdDoesNotExistInDb() {
        Long fakeId = 555L;
        org.mockito.Mockito.when(storageRepository.findById(fakeId)).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> storageService.loadResponseByMetadataId(fakeId))
                .isInstanceOf(StorageFileNotFoundException.class)
                .hasMessageContaining("File metadata not found with id: 555");
    }

    @Test
    void shouldLoadResponseByFilename_Success() throws IOException {
        String filename = "direct-file.txt";
        Files.writeString(sharedTempDir.resolve(filename), "direct access content");

        java.util.Map<String, Object> response = storageService.loadResponseByFilename(filename);

        assertThat(response).isNotNull();
        assertThat(response.get("filename")).isEqualTo(filename);
        assertThat(response.get("contentType")).isEqualTo("text/plain");

        org.springframework.core.io.Resource resource = (org.springframework.core.io.Resource) response.get("resource");
        assertThat(resource.exists()).isTrue();
    }

    @Test
    void shouldThrow404WhenFilenameDoesNotExistOnDisk() {
        String missingFilename = "ghost-file.jpg";

        assertThatThrownBy(() -> storageService.loadResponseByFilename(missingFilename))
                .isInstanceOf(StorageFileNotFoundException.class)
                .hasMessageContaining("Could not read file: " + missingFilename);
    }
}