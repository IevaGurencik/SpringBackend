package com.example.SpringBackend.controller;

import com.example.SpringBackend.model.ToDoEntity;
import com.example.SpringBackend.service.ToDoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/api")
public class ToDoController {
    private final ToDoService toDoService;

    @Autowired
    public ToDoController(ToDoService toDoService) {
        this.toDoService = toDoService;
    }

    @GetMapping("/todos")
    public List<ToDoEntity> findAll() {
        return toDoService.findAll();
    }

    @GetMapping("/todos/{id}")
    public ToDoEntity getToDo(@PathVariable long id) {
        return toDoService.findById(id);
    }

    @PostMapping("/todos")
    public ToDoEntity addToDo(@RequestBody ToDoEntity toDoEntity) {
        return toDoService.save(toDoEntity);
    }

    @DeleteMapping("/todos/{id}")
    public String deleteToDo(@PathVariable long id) {
        toDoService.deleteById(id);
        return "Deleted ToDoEntity id - " + id;

    }
}
