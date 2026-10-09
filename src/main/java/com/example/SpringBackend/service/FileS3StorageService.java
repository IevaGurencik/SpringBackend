package com.example.SpringBackend.service;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import com.example.SpringBackend.exception.StorageException;
import com.example.SpringBackend.exception.StorageFileNotFoundException;
import com.example.SpringBackend.model.FileMetadataEntity;
import com.example.SpringBackend.model.ToDoEntity;
import com.example.SpringBackend.repository.StorageRepository;
import com.example.SpringBackend.repository.ToDoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Profile("prod")
public class FileS3StorageService implements StorageService {

    private final StorageRepository storageRepository;
    private final ToDoRepository todoRepository;
    private final S3Client s3Client;
    private final String bucketName;
    private final S3Presigner s3Presigner;

    public FileS3StorageService(StorageRepository storageRepository,
                                ToDoRepository todoRepository,
                                S3Client s3Client,
                                S3Presigner s3Presigner,
                                @Value("${aws.s3.bucket-name}") String bucketName) {
        if (bucketName == null || bucketName.trim().isEmpty()) {
            throw new StorageException("S3 Bucket name cannot be empty.");
        }
        this.storageRepository = storageRepository;
        this.todoRepository = todoRepository;
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.bucketName = bucketName;
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

                    this.uploadToS3(file, storedFilename);

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

    private void uploadToS3(MultipartFile file, String filename) {
        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(filename)
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(putObjectRequest,
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
        } catch (IOException | S3Exception e) {
            throw new StorageException("Failed to store file in S3: " + filename, e);
        }
    }

    @Override
    public void store(MultipartFile file) {
        if (file.isEmpty()) {
            throw new StorageException("Failed to store empty file.");
        }
        String originalFilename = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
        this.uploadToS3(file, originalFilename);
    }

    @Override
    public Resource loadAsResource(String filename) {
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(filename)
                    .build();

            ResponseInputStream<GetObjectResponse> s3InputStream = s3Client.getObject(getObjectRequest);
            return new InputStreamResource(s3InputStream);
        } catch (NoSuchKeyException e) {
            throw new StorageFileNotFoundException("Could not read file from S3: " + filename, e);
        } catch (S3Exception e) {
            throw new StorageException("Error communicating with S3 while loading file: " + filename, e);
        }
    }

    @Override
    public Map<String, Object> loadResponseByFilename(String filename) {
        try {
            Resource resource = loadAsResource(filename);

            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder().bucket(bucketName).key(filename).build();
            HeadObjectResponse headObjectResponse = s3Client.headObject(headObjectRequest);
            String contentType = headObjectResponse.contentType() != null ? headObjectResponse.contentType() : "application/octet-stream";

            Map<String, Object> response = new HashMap<>();
            response.put("resource", resource);
            response.put("filename", filename);
            response.put("contentType", contentType);
            return response;
        } catch (S3Exception e) {
            throw new StorageFileNotFoundException("Could not read file metadata from S3: " + filename, e);
        }
    }

    @Override
    public List<String> loadAllDownloadUrls() {
        try {
            ListObjectsV2Request listObjectsV2Request = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .build();

            List<S3Object> objects = s3Client.listObjectsV2(listObjectsV2Request).contents();

            return objects.stream()
                    .map(s3Object -> {
                        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                                .bucket(bucketName)
                                .key(s3Object.key())
                                .build();

                        software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest presignRequest =
                                software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest.builder()
                                        .signatureDuration(java.time.Duration.ofMinutes(15))
                                        .getObjectRequest(getObjectRequest)
                                        .build();

                        return s3Presigner.presignGetObject(presignRequest).url().toString();
                    })
                    .collect(Collectors.toList());

        } catch (S3Exception e) {
            throw new StorageException("Failed to generate S3 download URLs", e);
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
        try {
            ListObjectsV2Request listObjectsRequest = ListObjectsV2Request.builder().bucket(bucketName).build();
            ListObjectsV2Response listObjectsResponse = s3Client.listObjectsV2(listObjectsRequest);

            if (!listObjectsResponse.contents().isEmpty()) {
                List<ObjectIdentifier> keysToDelete = listObjectsResponse.contents().stream()
                        .map(s3Object -> ObjectIdentifier.builder().key(s3Object.key()).build())
                        .collect(Collectors.toList());

                DeleteObjectsRequest deleteObjectsRequest = DeleteObjectsRequest.builder()
                        .bucket(bucketName)
                        .delete(Delete.builder().objects(keysToDelete).build())
                        .build();

                s3Client.deleteObjects(deleteObjectsRequest);
            }
        } catch (S3Exception e) {
            throw new StorageException("Failed to delete all files from S3 bucket", e);
        }
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
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(storedFilename)
                    .build();
            s3Client.deleteObject(deleteObjectRequest);
        } catch (S3Exception e) {
            System.err.println("Error while deleting the file from S3: " + storedFilename);
        }
    }

    @Override
    public void init() {
        try {
            HeadBucketRequest headBucketRequest = HeadBucketRequest.builder().bucket(bucketName).build();
            s3Client.headBucket(headBucketRequest);
        } catch (NoSuchBucketException e) {
            CreateBucketRequest createBucketRequest = CreateBucketRequest.builder().bucket(bucketName).build();
            s3Client.createBucket(createBucketRequest);
        } catch (S3Exception e) {
            throw new StorageException("Could not initialize S3 storage", e);
        }
    }
}
