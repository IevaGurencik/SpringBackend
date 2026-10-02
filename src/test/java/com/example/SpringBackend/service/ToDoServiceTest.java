package com.example.SpringBackend.service;

import com.example.SpringBackend.model.FileMetadataEntity;
import com.example.SpringBackend.model.ToDoEntity;
import com.example.SpringBackend.repository.ToDoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ToDoServiceTest {

    @Mock
    private ToDoRepository toDoRepository;

    @InjectMocks
    private ToDoService toDoService;
    private ToDoEntity sampleToDoEntity;
    private FileMetadataEntity sampleFile;

    @Mock
    private FileSystemStorageService storageService;

    @BeforeEach
    void setUp() {
        sampleToDoEntity = new ToDoEntity();
        sampleToDoEntity.setId(1L);
        sampleToDoEntity.setText("Test");
        sampleToDoEntity.setFiles(new ArrayList<>());

        sampleFile = new FileMetadataEntity();
        sampleFile.setId(29L);
        sampleFile.setFilename("sup nerd.txt");
        sampleFile.setStoredFilename("14828b72-3a4e-4db4-862e-15560d7a4788.txt");

        sampleToDoEntity.setFiles(List.of(sampleFile));
    }

    @Test
    void findAll_ReturnsListOfToDos() {
        List<ToDoEntity> toDoEntities = List.of(sampleToDoEntity);
        when(toDoRepository.findAll()).thenReturn(toDoEntities);
        List<ToDoEntity> result = toDoService.findAll();
        assertEquals(1, result.size());
        verify(toDoRepository, times(1)).findAll();
    }

    @Test
    void findById_ExistingId_ReturnsToDo() {
        when(toDoRepository.findById(1L)).thenReturn(Optional.of(sampleToDoEntity));
        ToDoEntity result = toDoService.findById(1);
        assertNotNull(result);
        assertEquals("Test", result.getText());
    }

    @Test
    void findById_NonExistingId_ThrowsException() {
        when(toDoRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> toDoService.findById(1));
    }

    @Test
    void save_ReturnsSavedToDo() {
        when(toDoRepository.save(any(ToDoEntity.class))).thenReturn(sampleToDoEntity);
        ToDoEntity result = toDoService.save(sampleToDoEntity);
        assertNotNull(result);
        verify(toDoRepository).save(sampleToDoEntity);
    }

    @Test
    void deleteById_NonExistingId_ThrowsException() {
        when(toDoRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> toDoService.deleteById(1L));

        verify(toDoRepository, times(1)).findById(1L);
        verify(toDoRepository, never()).delete(any(ToDoEntity.class));
        verify(storageService, never()).deletePhysicalFile(anyString());
    }

    @Test
    void deleteById_ShouldDeleteTodoAndItsPhysicalFiles() {
        long todoId = 1L;
        when(toDoRepository.findById(todoId)).thenReturn(Optional.of(sampleToDoEntity));
        toDoService.deleteById(todoId);
        verify(storageService, times(1)).deletePhysicalFile("14828b72-3a4e-4db4-862e-15560d7a4788.txt");
        verify(toDoRepository, times(1)).delete(sampleToDoEntity);
    }

    @Test
    void deleteById_ShouldWorkFine_WhenTodoHasNoFiles() {
        long todoId = 1L;
        sampleToDoEntity.setFiles(Collections.emptyList());
        when(toDoRepository.findById(todoId)).thenReturn(Optional.of(sampleToDoEntity));

        toDoService.deleteById(todoId);

        verify(storageService, never()).deletePhysicalFile(anyString());
        verify(toDoRepository, times(1)).delete(sampleToDoEntity);
    }

    @Test
    void deleteById_ShouldThrowException_WhenTodoNotFound() {
        long nonExistingId = 999L;
        when(toDoRepository.findById(nonExistingId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            toDoService.deleteById(nonExistingId);
        });

        assertEquals("ToDoEntity id not found - " + nonExistingId, exception.getMessage());

        verify(storageService, never()).deletePhysicalFile(anyString());
        verify(toDoRepository, never()).delete(any(ToDoEntity.class));
    }

}