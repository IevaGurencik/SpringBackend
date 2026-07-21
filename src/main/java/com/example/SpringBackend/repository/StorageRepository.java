package com.example.SpringBackend.repository;

import jakarta.annotation.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

public interface StorageRepository {

    void init();

    void store(MultipartFile file);

    Stream<Path> loadAll();

    List<String> loadAllDownloadUrls();

    Path load(String filename);

    Resource loadAsResource(String filename);

    void deleteAll();
}
