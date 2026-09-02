package com.example.SpringBackend.controller;

import com.example.SpringBackend.exception.GlobalExceptionHandler;
import com.example.SpringBackend.model.ToDoEntity;
import com.example.SpringBackend.model.FileMetadataEntity;
import com.example.SpringBackend.service.ToDoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ToDoControllerTest {
    private MockMvc mockMvc;
    private ToDoService toDoService;
    private ObjectMapper objectMapper;
    private ToDoEntity toDoEntity;

    @BeforeEach
    void setup() {
        toDoService = Mockito.mock(ToDoService.class);
        ToDoController controller = new ToDoController(toDoService);
        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();

        toDoEntity = new ToDoEntity();
        toDoEntity.setId(1L);
        toDoEntity.setText("Test Text");
        toDoEntity.setFiles(new ArrayList<>());
    }

    @Test
    void findAll_ReturnsToDoList() throws Exception {
        when(toDoService.findAll()).thenReturn(List.of(toDoEntity));
        mockMvc.perform(get("/api/todos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].text").value("Test Text"))
                .andExpect(jsonPath("$[0].files").isArray());
    }

    @Test
    void findAll_EmptyList_ReturnsEmptyArray() throws Exception {
        when(toDoService.findAll()).thenReturn(List.of());
        mockMvc.perform(get("/api/todos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0))
                .andExpect(content().json("[]"));
    }

    @Test
    void getToDo_ValidId_ReturnsToDo() throws Exception {
        when(toDoService.findById(1)).thenReturn(toDoEntity);
        mockMvc.perform(get("/api/todos/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("Test Text"))
                .andExpect(jsonPath("$.files").isArray());
    }

    @Test
    void getToDo_WithAttachedFiles_ReturnsJsonArrayWithFileMetadata() throws Exception {
        FileMetadataEntity fileMock = new FileMetadataEntity();
        fileMock.setId(29L);
        fileMock.setFilename("document.pdf");
        toDoEntity.getFiles().add(fileMock);

        when(toDoService.findById(1)).thenReturn(toDoEntity);

        mockMvc.perform(get("/api/todos/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.files.length()").value(1))
                .andExpect(jsonPath("$.files[0].id").value(29))
                .andExpect(jsonPath("$.files[0].filename").value("document.pdf"));
    }

    @Test
    void getToDo_InvalidId_Returns404() throws Exception {
        when(toDoService.findById(999))
                .thenThrow(new RuntimeException("ToDoEntity id not found - 999"));

        mockMvc.perform(get("/api/todos/999"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("ToDoEntity id not found - 999"));
    }

    @Test
    void addToDo_ReturnsSavedToDo() throws Exception {
        ToDoEntity savedToDoEntity = new ToDoEntity();
        savedToDoEntity.setId(5L);
        savedToDoEntity.setText("New Text");
        savedToDoEntity.setFiles(new ArrayList<>());

        when(toDoService.save(any(ToDoEntity.class))).thenReturn(savedToDoEntity);

        mockMvc.perform(post("/api/todos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(savedToDoEntity)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.text").value("New Text"));
    }

    @Test
    void deleteToDo_ValidId_ReturnsConfirmation() throws Exception {
        doNothing().when(toDoService).deleteById(1);

        mockMvc.perform(delete("/api/todos/1"))
                .andExpect(status().isOk())
                .andExpect(content().string("Deleted ToDoEntity id - 1"));

        verify(toDoService, times(1)).deleteById(1);
    }

    @Test
    void deleteToDo_InvalidId_Returns404() throws Exception {
        doThrow(new RuntimeException("ToDoEntity id not found - 999")).when(toDoService).deleteById(999);

        mockMvc.perform(delete("/api/todos/999"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("ToDoEntity id not found - 999"));
    }

}