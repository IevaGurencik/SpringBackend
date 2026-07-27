package com.example.SpringBackend.exception;

import org.springframework.web.bind.annotation.ControllerAdvice;

@ControllerAdvice
public class StorageFileNotFoundException extends StorageException {

    public StorageFileNotFoundException(String message) {
        super(message);
    }

    public StorageFileNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
