package com.afetch.web.api;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.afetch.integration.MediaStorageService;

@RestController
@RequestMapping("/api/media")
public class MediaApiController {

    private static final Logger log = LoggerFactory.getLogger(MediaApiController.class);
    private final MediaStorageService mediaStorageService;

    public MediaApiController(MediaStorageService mediaStorageService) {
        this.mediaStorageService = mediaStorageService;
    }

    @PostMapping("/upload")
    public Map<String, String> upload(@RequestParam("file") MultipartFile file,
                                      @RequestParam(defaultValue = "media") String folder) {
        log.info("[MediaApiController] POST /api/media/upload - file: {}, folder: {}, size: {}", file.getOriginalFilename(), folder, file.getSize());
        String url = mediaStorageService.upload(file, folder);
        log.info("[MediaApiController] ✓ Upload successful, URL: {}", url);
        return Map.of("url", url);
    }
}
