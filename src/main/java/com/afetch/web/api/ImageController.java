package com.afetch.web.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.afetch.service.StorageService;
import com.afetch.web.dto.ImageUploadResponse;

@RestController
@RequestMapping("/api/images")
public class ImageController {

    private static final Logger log = LoggerFactory.getLogger(ImageController.class);
    private final StorageService storageService;

    public ImageController(StorageService storageService) {
        this.storageService = storageService;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImageUploadResponse> uploadImage(@RequestParam("file") MultipartFile file) {
        log.info("[ImageController] POST /api/images/upload - file: {}, size: {}", file.getOriginalFilename(), file.getSize());
        String url = storageService.upload(file);
        log.info("[ImageController] ✓ Upload successful, URL: {}", url);
        return ResponseEntity.status(HttpStatus.CREATED).body(new ImageUploadResponse(url));
    }
}
