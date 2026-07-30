package com.example.SpringBackend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "file_metadata")
public class FileMetadataEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "filename", nullable = false, unique = true, length = 255)
    private String filename;

    @Column(name = "download_url", nullable = false, length = 512)
    private String downloadUrl;

    @Column(name = "upload_time", nullable = false, insertable = false, updatable = false)
    private LocalDateTime uploadTime = LocalDateTime.now();

    @Column(name = "file_size_bytes", nullable = false)
    private Long fileSizeBytes;

    public FileMetadataEntity(Long id, String filename, String downloadUrl, LocalDateTime uploadTime, Long fileSizeBytes) {
        this.id = id;
        this.filename = filename;
        this.downloadUrl = downloadUrl;
        this.uploadTime = uploadTime;
        this.fileSizeBytes = fileSizeBytes;
    }

    public FileMetadataEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public LocalDateTime getUploadTime() {
        return uploadTime;
    }

    public void setUploadTime(LocalDateTime uploadTime) {
        this.uploadTime = uploadTime;
    }

    public Long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public void setFileSizeBytes(Long fileSizeBytes) {
        this.fileSizeBytes = fileSizeBytes;
    }

    @Override
    public String toString() {
        return "FileMetadataEntity{" +
                "id=" + id +
                ", filename='" + filename + '\'' +
                ", downloadUrl='" + downloadUrl + '\'' +
                ", uploadTime=" + uploadTime +
                ", fileSizeBytes=" + fileSizeBytes +
                '}';
    }
}

