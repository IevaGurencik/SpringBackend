package com.example.SpringBackend.service;

import com.example.SpringBackend.model.FileMetadataEntity;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface StorageService {

    void init();

    void store(MultipartFile file);

    void store(MultipartFile[] files, Long todoId);

    FileMetadataEntity findMetadataById(Long id);

    Resource loadAsResource(String filename);

    Map<String, Object> loadResponseByFilename(String filename);

    Map<String, Object> loadResponseByMetadataId(Long id);

    void deleteAll();

    void deleteByMetadataId(Long id);

    void deletePhysicalFile(String storedFilename);

    List<String> loadAllDownloadUrls();
}

