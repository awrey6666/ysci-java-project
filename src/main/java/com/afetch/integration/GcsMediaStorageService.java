package com.afetch.integration;

import com.afetch.config.AfetchProperties;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "afetch.gcs.enabled", havingValue = "true")
public class GcsMediaStorageService implements MediaStorageService {

    private static final Set<String> ALLOWED = Set.of("image/jpeg", "image/png", "image/gif", "image/webp");

    private final Storage storage;
    private final String bucket;

    public GcsMediaStorageService(AfetchProperties properties) {
        this.storage = StorageOptions.getDefaultInstance().getService();
        this.bucket = properties.getGcs().getBucket();
    }

    @Override
    public String upload(MultipartFile file, String folder) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED.contains(contentType)) {
            throw new IllegalArgumentException("Unsupported file type");
        }
        String objectName = folder + "/" + UUID.randomUUID();
        try {
            BlobInfo blobInfo = BlobInfo.newBuilder(bucket, objectName)
                    .setContentType(contentType)
                    .build();
            storage.create(blobInfo, file.getBytes());
            return String.format("https://storage.googleapis.com/%s/%s", bucket, objectName);
        } catch (IOException e) {
            throw new IllegalStateException("GCS upload failed", e);
        }
    }
}
