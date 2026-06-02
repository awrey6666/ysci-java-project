package com.afetch.integration;

import org.springframework.web.multipart.MultipartFile;

public interface MediaStorageService {

    String upload(MultipartFile file, String folder);
}
