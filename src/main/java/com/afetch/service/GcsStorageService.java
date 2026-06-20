package com.afetch.service;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.multipart.MultipartFile;

import com.afetch.config.AfetchProperties;
import com.afetch.domain.entity.UploadedImage;
import com.afetch.repository.UploadedImageRepository;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageException;

@Service
public class GcsStorageService implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(GcsStorageService.class);
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp"
    );

    private static final long MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024L;
    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp"
    );

    private final Storage storage;
    private final UploadedImageRepository imageRepository;
    private final String bucket;

    public GcsStorageService(ObjectProvider<Storage> storageProvider,
                             UploadedImageRepository imageRepository,
                             AfetchProperties properties) {
        this.storage = storageProvider.getIfAvailable();
        this.imageRepository = imageRepository;
        this.bucket = properties.getGcs().getBucket();
        if (this.storage == null) {
            log.warn("GCS Storage bean not available. GCS uploads will be unavailable at runtime.");
        } else {
            log.info("GcsStorageService initialized and ready for uploads to bucket: {}", bucket);
        }
    }

    @Override
    public String upload(MultipartFile file) {
        if (storage == null) {
            throw new com.afetch.exception.GcsUnavailableException("Google Cloud Storage is not configured");
        }
        validateFile(file);

        String contentType = file.getContentType();
        String extension = EXTENSIONS.get(contentType);
        String objectName = UUID.randomUUID().toString() + extension;
        
        log.debug("Uploading to GCS: bucket={}, object={}, contentType={}, size={}", 
                  bucket, objectName, contentType, file.getSize());
        
        BlobInfo blobInfo = BlobInfo.newBuilder(bucket, objectName)
                .setContentType(contentType)
                .build();

        try {
            storage.create(blobInfo, file.getBytes());
            log.info("File uploaded to GCS: gs://{}/{}", bucket, objectName);
        } catch (StorageException e) {
            log.error("✗ GCS upload failed: {}", e.getMessage(), e);
            throw new IllegalStateException("Failed to upload image to Google Cloud Storage: " + e.getMessage(), e);
        } catch (IOException e) {
            log.error("✗ Failed to read file bytes: {}", e.getMessage(), e);
            throw new IllegalStateException("Failed to read file: " + e.getMessage(), e);
        }

        String publicUrl = String.format("https://storage.googleapis.com/%s/%s", bucket, objectName);
        log.info("✓ Public URL generated: {}", publicUrl);
        persistImageUrl(publicUrl);
        log.info("✓ Image URL persisted to database");
        return publicUrl;
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File must not be empty");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("File must be smaller than 10 MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Unsupported file format. Allowed formats: jpg, jpeg, png, webp");
        }
    }

    private void persistImageUrl(String url) {
        UploadedImage image = new UploadedImage();
        image.setUrl(url);
        imageRepository.save(image);
    }
}
