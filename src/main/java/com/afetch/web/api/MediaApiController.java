package com.afetch.web.api;

import com.afetch.integration.MediaStorageService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/media")
public class MediaApiController {

    private final MediaStorageService mediaStorageService;

    public MediaApiController(MediaStorageService mediaStorageService) {
        this.mediaStorageService = mediaStorageService;
    }

    @PostMapping("/upload")
    public Map<String, String> upload(@RequestParam("file") MultipartFile file,
                                      @RequestParam(defaultValue = "media") String folder) {
        String url = mediaStorageService.upload(file, folder);
        return Map.of("url", url);
    }
}
