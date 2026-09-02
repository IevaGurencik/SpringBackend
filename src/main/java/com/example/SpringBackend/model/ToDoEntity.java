package com.example.SpringBackend.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "to_do_entity")
public class ToDoEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "text", length = 255)
    private String text;

    @OneToMany(mappedBy = "todo", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<FileMetadataEntity> files = new ArrayList<>();

    public ToDoEntity(long id, String text, List<FileMetadataEntity> files) {
        this.id = id;
        this.text = text;
        this.files = files;
    }

    public ToDoEntity() {
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public List<FileMetadataEntity> getFiles() {
        return files;
    }

    public void setFiles(List<FileMetadataEntity> files) {
        this.files = files;
    }

    @Override
    public String toString() {
        return "ToDoEntity{" +
                "id=" + id +
                ", text='" + text + '\'' +
                '}';
    }


}
