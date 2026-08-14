package com.example.SpringBackend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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

    @ManyToOne
    @JoinColumn(name = "todo_id")
    private ToDoEntity todo;

    public FileMetadataEntity(Long id, String filename, String downloadUrl, LocalDateTime uploadTime, Long fileSizeBytes) {
        this.id = id;
        this.filename = filename;
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

    @Override
    public String toString() {
        return "FileMetadataEntity{" +
                "id=" + id +
                ", filename='" + filename + '\'' +
                '}';
    }
}

