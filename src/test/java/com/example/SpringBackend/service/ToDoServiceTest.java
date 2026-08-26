package com.example.SpringBackend.service;

import com.example.SpringBackend.model.ToDoEntity;
import com.example.SpringBackend.repository.ToDoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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

    @BeforeEach
    void setUp() {
        sampleToDoEntity = new ToDoEntity();
        sampleToDoEntity.setId(1L);
        sampleToDoEntity.setText("Test");
        sampleToDoEntity.setFiles(new ArrayList<>());
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
        when(toDoRepository.existsById(1L)).thenReturn(false);

        assertThrows(RuntimeException.class, () -> toDoService.deleteById(1L));

        verify(toDoRepository, times(1)).existsById(1L);
        verify(toDoRepository, times(0)).deleteById(1L);
    }
}