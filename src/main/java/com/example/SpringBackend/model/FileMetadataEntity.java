package com.example.SpringBackend.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;

@Entity
@Table(name = "file_metadata")
public class FileMetadataEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "filename", nullable = false, length = 255)
    private String filename;

    @Column(name = "stored_filename", nullable = false, unique = true, length = 255)
    private String storedFilename;

    @ManyToOne
    @JoinColumn(name = "todo_id")
    @JsonBackReference
    private ToDoEntity todo;

    public FileMetadataEntity() {
    }

    public FileMetadataEntity(Long id, String filename, ToDoEntity todo) {
        this.id = id;
        this.filename = filename;
        this.todo = todo;
        if (this.storedFilename == null && filename != null) {
            this.storedFilename = java.util.UUID.randomUUID().toString() + "_" + filename;
        }
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
        if (this.storedFilename == null && filename != null) {
            this.storedFilename = java.util.UUID.randomUUID().toString() + "_" + filename;
        }
    }

    public String getStoredFilename() {
        return storedFilename;
    }

    public void setStoredFilename(String storedFilename) {
        this.storedFilename = storedFilename;
    }

    public ToDoEntity getTodo() {
        return todo;
    }

    public void setTodo(ToDoEntity todo) {
        this.todo = todo;
    }

    @Override
    public String toString() {
        return "FileMetadataEntity{" +
                "id=" + id +
                ", filename='" + filename + '\'' +
                ", storedFilename='" + storedFilename + '\'' +
                ", todoId=" + (todo != null ? todo.getId() : null) +
                '}';
    }
}