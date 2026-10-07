package com.example.SpringBackend.service;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import com.example.SpringBackend.exception.StorageException;
import com.example.SpringBackend.exception.StorageFileNotFoundException;
import com.example.SpringBackend.controller.FileUploadController;
import com.example.SpringBackend.model.FileMetadataEntity;
import com.example.SpringBackend.model.ToDoEntity;
import com.example.SpringBackend.repository.StorageRepository;
import com.example.SpringBackend.repository.ToDoRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.FileSystemUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;

@Service
@Profile("!prod")
public class FileLocalStorageService implements StorageService {

    private final StorageRepository storageRepository;
    private final ToDoRepository todoRepository;
    private final Path rootLocation;

    public FileLocalStorageService(@Value("${storage.location}") String location,
                                   StorageRepository storageRepository,
                                   ToDoRepository todoRepository) {
        if (location == null || location.trim().isEmpty()) {
            throw new StorageException("File upload location cannot be empty.");
        }
        this.rootLocation = Paths.get(location);
        this.storageRepository = storageRepository;
        this.todoRepository = todoRepository;
    }

    @Override
    @Transactional
    public void store(MultipartFile[] files, Long todoId) {
        if (files == null || files.length == 0) {
            throw new StorageException("No files provided for upload.");
        }
        ToDoEntity todo = todoRepository.findById(todoId)
                .orElseThrow(() -> new StorageException("Cannot upload files. ToDo not found with id: " + todoId));

        Arrays.stream(files)
                .filter(file -> !file.isEmpty())
                .forEach(file -> {
                    String originalFilename = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
                    String fileExtension = StringUtils.getFilenameExtension(originalFilename);
                    String storedFilename = UUID.randomUUID().toString() + (fileExtension != null ? "." + fileExtension : "");
                    this.storeFileToDisk(file, storedFilename);

                    FileMetadataEntity metadata = new FileMetadataEntity();
                    metadata.setFilename(originalFilename);
                    metadata.setStoredFilename(storedFilename);
                    metadata.setTodo(todo);

                    storageRepository.save(metadata);
                });
    }

    @Override
    public FileMetadataEntity findMetadataById(Long id) {
        return storageRepository.findById(id)
                .orElseThrow(() -> new StorageFileNotFoundException("File metadata not found with id: " + id));
    }

    private void storeFileToDisk(MultipartFile file, String filename) {
        try {
            Path destinationFile = this.rootLocation.resolve(Paths.get(filename)).normalize().toAbsolutePath();
            if (!destinationFile.getParent().equals(this.rootLocation.toAbsolutePath())) {
                throw new StorageException("Cannot store file outside current directory.");
            }
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new StorageException("Failed to store file " + filename, e);
        }
    }

    @Override
    public void store(MultipartFile file) {
        if (file.isEmpty()) {
            throw new StorageException("Failed to store empty file.");
        }
        String originalFilename = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
        this.storeFileToDisk(file, originalFilename);
    }

    @Override
    public Resource loadAsResource(String filename) {
        try {
            Path file = rootLocation.resolve(filename);
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() || resource.isReadable()) {
                return resource;
            } else {
                throw new StorageFileNotFoundException("Could not read file: " + filename);
            }
        } catch (MalformedURLException e) {
            throw new StorageFileNotFoundException("Could not read file: " + filename, e);
        }
    }

    @Override
    public Map<String, Object> loadResponseByFilename(String filename) {
        Resource resource = loadAsResource(filename);
        String contentType = "application/octet-stream";
        try {
            String probed = Files.probeContentType(rootLocation.resolve(filename));
            if (probed != null) contentType = probed;
        } catch (IOException ignored) {
        }

        Map<String, Object> response = new HashMap<>();
        response.put("resource", resource);
        response.put("filename", resource.getFilename() != null ? resource.getFilename() : "file");
        response.put("contentType", contentType);
        return response;
    }

    @Override
    public List<String> loadAllDownloadUrls() {
        try {
            return Files.walk(this.rootLocation, 1)
                    .filter(path -> !path.equals(this.rootLocation))
                    .map(this.rootLocation::relativize)
                    .map(path -> MvcUriComponentsBuilder.fromMethodName(FileUploadController.class,
                            "serveFile", path.getFileName().toString()).build().toUri().toString())
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new StorageException("Failed to generate download URLs", e);
        }
    }

    @Override
    public Map<String, Object> loadResponseByMetadataId(Long id) {
        FileMetadataEntity metadata = storageRepository.findById(id)
                .orElseThrow(() -> new StorageFileNotFoundException("File metadata not found with id: " + id));

        Map<String, Object> response = loadResponseByFilename(metadata.getStoredFilename());
        response.put("filename", metadata.getFilename() != null ? metadata.getFilename() : "file");
        return response;
    }

    @Override
    public void deleteAll() {
        FileSystemUtils.deleteRecursively(rootLocation.toFile());
    }

    @Override
    @Transactional
    public void deleteByMetadataId(Long id) {
        FileMetadataEntity metadata = storageRepository.findById(id)
                .orElseThrow(() -> new StorageFileNotFoundException("Could not find file with id: " + id));
        this.deletePhysicalFile(metadata.getStoredFilename());
        storageRepository.delete(metadata);
    }

    @Override
    public void deletePhysicalFile(String storedFilename) {
        try {
            Files.deleteIfExists(rootLocation.resolve(storedFilename));
        } catch (IOException e) {
            System.err.println("Error while deleting the file: " + storedFilename);
        }
    }

    @Override
    public void init() {
        try {
            Files.createDirectories(rootLocation);
        } catch (IOException e) {
            throw new StorageException("Could not initialize storage location", e);
        }
    }
}