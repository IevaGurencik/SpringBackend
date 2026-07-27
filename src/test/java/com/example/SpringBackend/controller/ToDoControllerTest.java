package com.example.SpringBackend.controller;

import com.example.SpringBackend.exception.GlobalExceptionHandler;
import com.example.SpringBackend.model.ToDoEntity;
import com.example.SpringBackend.service.ToDoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
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
        toDoEntity = new ToDoEntity(1, "Test Text");
    }

    @Test
    void findAll_ReturnsToDoList() throws Exception {
        when(toDoService.findAll()).thenReturn(List.of(toDoEntity));
        mockMvc.perform(get("/api/todos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].text").value("Test Text"));
    }

    @Test
    void getToDo_ValidId_ReturnsToDo() throws Exception {
        when(toDoService.findById(1)).thenReturn(toDoEntity);
        mockMvc.perform(get("/api/todos/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("Test Text"));
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
        ToDoEntity savedToDoEntity = new ToDoEntity(5, "New Text");

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
        when(toDoService.findById(1)).thenReturn(toDoEntity);
        Mockito.doNothing().when(toDoService).deleteById(1);
        mockMvc.perform(delete("/api/todos/1"))
                .andExpect(status().isOk())
                .andExpect(content().string("Deleted ToDoEntity id - 1"));
    }

    @Test
    void deleteToDo_InvalidId_Returns404() throws Exception {
        doThrow(new RuntimeException()).when(toDoService).deleteById(999);
        mockMvc.perform(delete("/api/todos/999"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(""));
    }
}