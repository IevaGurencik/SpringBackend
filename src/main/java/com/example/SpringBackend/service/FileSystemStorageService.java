package com.example.SpringBackend.service;

import com.example.SpringBackend.config.StorageProperties;
import com.example.SpringBackend.exception.StorageException;
import com.example.SpringBackend.exception.StorageFileNotFoundException;
import com.example.SpringBackend.model.FileMetadataEntity;
import com.example.SpringBackend.model.ToDoEntity;
import com.example.SpringBackend.repository.StorageRepository;
import com.example.SpringBackend.repository.ToDoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.FileSystemUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.stream.Stream;

@Service
public class FileSystemStorageService {

    private final StorageRepository storageRepository;
    private final ToDoRepository todoRepository;
    private final Path rootLocation;

    @Autowired(required = false)
    private S3Client s3Client;

    @Autowired(required = false)
    private S3Presigner s3Presigner;

    @Value("${storage.bucket-name:}")
    private String bucketName;

    public FileSystemStorageService(StorageProperties properties,
                                    StorageRepository storageRepository,
                                    ToDoRepository todoRepository) {
        if (properties.getLocation().trim().isEmpty()) {
            throw new StorageException("File upload location cannot be empty.");
        }
        this.rootLocation = Paths.get(properties.getLocation());
        this.storageRepository = storageRepository;
        this.todoRepository = todoRepository;
    }

    public void init() {
        try {
            Files.createDirectories(rootLocation);
        } catch (IOException e) {
            throw new StorageException("Could not initialize storage", e);
        }
    }

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

                    if (s3Client != null && !bucketName.isEmpty()) {
                        this.uploadToS3(file, storedFilename);
                    } else {
                        this.storeFileToDisk(file, storedFilename);
                    }

                    FileMetadataEntity metadata = new FileMetadataEntity();
                    metadata.setFilename(originalFilename);
                    metadata.setStoredFilename(storedFilename);
                    metadata.setTodo(todo);

                    storageRepository.save(metadata);
                });
    }

    private void uploadToS3(MultipartFile file, String s3Key) {
        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .contentType(file.getContentType())
                    .build();
            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
        } catch (IOException e) {
            throw new StorageException("Failed to upload file to S3: " + s3Key, e);
        }
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

    public void store(MultipartFile file) {
        if (file.isEmpty()) {
            throw new StorageException("Failed to store empty file.");
        }
        String originalFilename = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
        if (s3Client != null && !bucketName.isEmpty()) {
            this.uploadToS3(file, originalFilename);
        } else {
            this.storeFileToDisk(file, originalFilename);
        }
    }

    public FileMetadataEntity findMetadataById(Long id) {
        return storageRepository.findById(id)
                .orElseThrow(() -> new StorageFileNotFoundException("Metadata not found"));
    }

    public Path load(String filename) {
        return rootLocation.resolve(filename);
    }

    public Stream<Path> loadAll() {
        try {
            return Files.walk(this.rootLocation, 1)
                    .filter(path -> !path.equals(this.rootLocation))
                    .map(this.rootLocation::relativize);
        } catch (IOException e) {
            throw new StorageException("Failed to read stored files", e);
        }
    }

    public List<String> loadAllDownloadUrls() {
        return storageRepository.findAll().stream()
                .map(metadata -> "/api/files/id/" + metadata.getId())
                .toList();
    }

    public Resource loadAsResource(String filename) {
        try {
            if (s3Client != null && s3Presigner != null) {
                String presignedUrl = generatePresignedUrl(filename);
                return new UrlResource(presignedUrl);
            }

            Path file = load(filename);
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

    private String generatePresignedUrl(String key) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder().bucket(bucketName).key(key).build();
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(15))
                .getObjectRequest(getObjectRequest).build();
        return s3Presigner.presignGetObject(presignRequest).url().toString();
    }

    public Map<String, Object> loadResponseByFilename(String filename) {
        Resource resource = loadAsResource(filename);
        String contentType = detectContentType(resource);

        return Map.of(
                "filename", filename,
                "resource", resource,
                "contentType", contentType
        );
    }

    public Map<String, Object> loadResponseByMetadataId(Long id) {
        FileMetadataEntity metadata = storageRepository.findById(id)
                .orElseThrow(() -> new StorageFileNotFoundException("Could not find file with id: " + id));

        Resource resource = loadAsResource(metadata.getStoredFilename());
        String contentType = detectContentType(resource);

        return Map.of(
                "filename", metadata.getFilename(),
                "resource", resource,
                "contentType", contentType
        );
    }

    private String detectContentType(Resource resource) {
        try {
            String contentType = Files.probeContentType(resource.getFile().toPath());
            if (contentType != null) {
                return contentType;
            }
        } catch (Exception e) {

        }
        return "application/octet-stream";
    }

    @Transactional
    public void deleteByMetadataId(Long id) {
        FileMetadataEntity metadata = storageRepository.findById(id)
                .orElseThrow(() -> new StorageFileNotFoundException("Could not find file with id: " + id));

        String storedFilename = metadata.getStoredFilename();

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    executePhysicalDeletion(storedFilename);
                }
            });
        } else {
            executePhysicalDeletion(storedFilename);
        }
        storageRepository.delete(metadata);
    }
    private void executePhysicalDeletion(String storedFilename) {
        if (s3Client != null && !bucketName.isEmpty()) {
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(storedFilename)
                    .build();
            s3Client.deleteObject(deleteRequest);
        } else {
            Path file = rootLocation.resolve(storedFilename);
            try {
                Files.deleteIfExists(file);
            } catch (IOException e) {
                throw new StorageException("Failed to delete local file: " + storedFilename, e);
            }
        }
    }
    public void deleteAll() {
        if (s3Client == null) {
            FileSystemUtils.deleteRecursively(rootLocation.toFile());
        }
    }
}