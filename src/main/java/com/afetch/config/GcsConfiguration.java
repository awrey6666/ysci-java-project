package com.afetch.config;

import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;

@Configuration
@ConditionalOnProperty(prefix = "afetch.gcs", name = "enabled", havingValue = "true")
public class GcsConfiguration {

    private static final Logger log = LoggerFactory.getLogger(GcsConfiguration.class);

    @Bean
    public Storage storage(AfetchProperties properties) {
        validateCredentialsFile();

        String projectId = properties.getGcs().getProjectId();
        String bucket = properties.getGcs().getBucket();

        log.info("Initializing Google Cloud Storage (project={}, bucket={})", projectId, bucket);

        Storage storage = StorageOptions.newBuilder()
                .setProjectId(projectId)
                .build()
                .getService();

        var bucketInfo = storage.get(bucket);
        if (bucketInfo == null) {
            throw new IllegalStateException("GCS bucket not found: " + bucket);
        }

        log.info("Successfully connected to GCS bucket: {}", bucket);
        return storage;
    }

    private void validateCredentialsFile() {
        String credentialsPath = System.getenv("GOOGLE_APPLICATION_CREDENTIALS");
        if (credentialsPath == null || credentialsPath.isBlank()) {
            throw new IllegalStateException(
                    "GOOGLE_APPLICATION_CREDENTIALS is not set. "
                            + "Run: gcloud auth application-default login");
        }

        Path path = Path.of(credentialsPath);
        if (!Files.isRegularFile(path)) {
            throw new IllegalStateException(
                    "GCS credentials file not found or is a directory: " + credentialsPath + ". "
                            + "Run: gcloud auth application-default login");
        }
    }
}
