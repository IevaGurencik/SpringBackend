package com.example.SpringBackend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

//    @PostMapping("/login")
//    public ResponseEntity<String> login(
//            @RequestParam String email,
//            @RequestParam String password) {
//        String token = authService.login(email, password);
//        return ResponseEntity.ok(token);
//    }
}
