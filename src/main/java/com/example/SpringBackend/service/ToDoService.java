package com.example.SpringBackend.service;

import com.example.SpringBackend.model.ToDoEntity;
import com.example.SpringBackend.repository.ToDoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ToDoService {
    private final ToDoRepository toDoRepository;
    private final FileSystemStorageService storageService;

    public ToDoService(ToDoRepository toDoRepository, FileSystemStorageService storageService) {
        this.toDoRepository = toDoRepository;
        this.storageService = storageService;
    }

    public List<ToDoEntity> findAll() {
        return toDoRepository.findAll();
    }

    public ToDoEntity findById(long id) {
        Optional<ToDoEntity> result = toDoRepository.findById(id);
        return result.orElseThrow(() -> new RuntimeException("Did not find ToDoEntity id - " + id));
    }

    public ToDoEntity save(ToDoEntity theToDosEntity) {
        return toDoRepository.save(theToDosEntity);
    }

    @Transactional
    public void deleteById(long id) {
        ToDoEntity toDoEntity = toDoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("ToDoEntity id not found - " + id));

        if (toDoEntity.getFiles() != null) {
            toDoEntity.getFiles().forEach(file -> {
                storageService.deletePhysicalFile(file.getStoredFilename());
            });
        }

        toDoRepository.delete(toDoEntity);
    }
}