package com.afetch.integration;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.multipart.MultipartFile;

import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageException;

@Service
public class GcsMediaStorageService implements MediaStorageService {

    private static final Logger log = LoggerFactory.getLogger(GcsMediaStorageService.class);
    private static final String BUCKET = "java_project_bucket";
    private static final Set<String> ALLOWED = Set.of("image/jpeg", "image/png", "image/gif", "image/webp");
    private static final long MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024L;

    private final Storage storage;

    public GcsMediaStorageService(ObjectProvider<com.google.cloud.storage.Storage> storageProvider) {
        this.storage = storageProvider.getIfAvailable();
        if (this.storage == null) {
            log.warn("GCS Storage bean not available. GCS uploads will be unavailable at runtime.");
        } else {
            log.info("✓ GcsMediaStorageService initialized and ready for uploads to bucket: {}", BUCKET);
        }
    }

    @Override
    public String upload(MultipartFile file, String folder) {
        if (storage == null) {
            throw new com.afetch.exception.GcsUnavailableException("Google Cloud Storage is not configured");
        }
        validateFile(file);
        
        String contentType = file.getContentType();
        String objectName = folder + "/" + UUID.randomUUID();
        
        log.debug("Uploading to GCS: bucket={}, object={}, contentType={}, size={}", 
                  BUCKET, objectName, contentType, file.getSize());
        
        try {
            BlobInfo blobInfo = BlobInfo.newBuilder(BUCKET, objectName)
                    .setContentType(contentType)
                    .build();
            storage.create(blobInfo, file.getBytes());
            log.info("✓ File uploaded to GCS: gs://{}/{}", BUCKET, objectName);
            
            String publicUrl = String.format("https://storage.googleapis.com/%s/%s", BUCKET, objectName);
            log.info("✓ Public URL generated: {}", publicUrl);
            return publicUrl;
        } catch (StorageException e) {
            log.error("✗ GCS upload failed: {}", e.getMessage(), e);
            throw new IllegalStateException("GCS upload failed: " + e.getMessage(), e);
        } catch (IOException e) {
            log.error("✗ Failed to read file bytes: {}", e.getMessage(), e);
            throw new IllegalStateException("Failed to read file: " + e.getMessage(), e);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("File must be smaller than 10 MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED.contains(contentType)) {
            throw new IllegalArgumentException("Unsupported file type. Allowed: jpeg, png, gif, webp");
        }
    }
}
