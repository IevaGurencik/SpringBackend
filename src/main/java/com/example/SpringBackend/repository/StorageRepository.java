package com.example.SpringBackend.repository;

import org.springframework.core.io.Resource;
import org.springframework.stereotype.Repository;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

@Repository
public interface StorageRepository {

    void init();

    void store(MultipartFile file);

    Stream<Path> loadAll();

    List<String> loadAllDownloadUrls();

    Path load(String filename);

    Resource loadAsResource(String filename);

    void deleteAll();
}
