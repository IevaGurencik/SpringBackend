package com.example.SpringBackend.repository;

import com.example.SpringBackend.model.FileMetadataEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StorageRepository extends JpaRepository<FileMetadataEntity, Long> {

}
