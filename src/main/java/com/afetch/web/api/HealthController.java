package com.afetch.web.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.afetch.config.AfetchProperties;
import com.google.cloud.storage.Storage;
import org.springframework.beans.factory.ObjectProvider;

import java.util.HashMap;
import java.util.Map;

/**
 * Diagnostic endpoint to verify GCS configuration and upload readiness.
 * GET /api/health/gcs - returns GCS status
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {

    private static final Logger log = LoggerFactory.getLogger(HealthController.class);

    private final Storage storage;
    private final String bucket;

    public HealthController(ObjectProvider<Storage> storageProvider, AfetchProperties properties) {
        this.storage = storageProvider.getIfAvailable();
        this.bucket = properties.getGcs().getBucket();
    }

    @GetMapping("/gcs")
    public ResponseEntity<Map<String, Object>> gcsHealth() {
        Map<String, Object> health = new HashMap<>();
        
        try {
            boolean available = storage != null;
            health.put("available", available);
            health.put("bucket", bucket);

            if (!available) {
                health.put("bucketAccessible", false);
                health.put("message", "Google Cloud Storage not configured");
                log.warn("GCS storage bean not available for health check");
                return ResponseEntity.ok(health);
            }

            var bucketInfo = storage.get(bucket);
            boolean bucketAccessible = bucketInfo != null;
            health.put("bucketAccessible", bucketAccessible);
            health.put("projectId", storage.getOptions().getProjectId());

            if (bucketAccessible) {
                health.put("message", "GCS ready - uploads will go to Google Cloud Storage");
                log.info("GCS health check passed");
            } else {
                health.put("message", "Bucket not found: " + bucket);
                log.warn("Bucket '{}' not found", bucket);
            }

            return ResponseEntity.ok(health);
        } catch (Exception e) {
            health.put("available", false);
            health.put("bucketAccessible", false);
            health.put("message", "GCS verification failed: " + e.getMessage());
            log.error("✗ GCS health check failed", e);
            return ResponseEntity.ok(health);
        }
    }
}
