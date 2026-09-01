package com.example.SpringBackend.controller;

import com.example.SpringBackend.exception.StorageFileNotFoundException;
import com.example.SpringBackend.service.FileSystemStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/api")
public class FileUploadController {

    private final FileSystemStorageService storageService;

    @Autowired
    public FileUploadController(FileSystemStorageService storageService) {
        this.storageService = storageService;
    }

    @GetMapping("/files")
    @ResponseBody
    public List<String> listUploadedFiles() {
        return storageService.loadAllDownloadUrls();
    }

    @GetMapping("/files/{filename:.+}")
    public ResponseEntity<Resource> serveFile(@PathVariable String filename) {
        Map<String, Object> fileResponse = storageService.loadResponseByFilename(filename);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileResponse.get("filename") + "\"")
                .contentType(MediaType.parseMediaType((String) fileResponse.get("contentType")))
                .body((Resource) fileResponse.get("resource"));
    }

    @GetMapping(value = "/files/id/{id}")
    public ResponseEntity<Resource> serveFileById(@PathVariable Long id) {
        Map<String, Object> fileResponse = storageService.loadResponseByMetadataId(id);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileResponse.get("filename") + "\"")
                .contentType(MediaType.parseMediaType((String) fileResponse.get("contentType")))
                .body((Resource) fileResponse.get("resource"));
    }

    @PostMapping("/files")
    public ResponseEntity<?> handleFileUpload(@RequestParam("files") MultipartFile[] files,
                                              @RequestParam("todoId") Long todoId) {
        storageService.store(files, todoId);

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "message", "Successfully uploaded " + files.length + " files!",
                "todoId", todoId
        ));
    }

    @ExceptionHandler(StorageFileNotFoundException.class)
    public ResponseEntity<?> handleStorageFileNotFound(StorageFileNotFoundException exc) {
        return ResponseEntity.notFound().build();
    }
}