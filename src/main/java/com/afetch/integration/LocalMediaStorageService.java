package com.afetch.integration;

import com.afetch.config.AfetchProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "afetch.gcs.enabled", havingValue = "false", matchIfMissing = true)
public class LocalMediaStorageService implements MediaStorageService {

    private static final Set<String> ALLOWED = Set.of("image/jpeg", "image/png", "image/gif", "image/webp");

    private final Path uploadDir;

    public LocalMediaStorageService(AfetchProperties properties) throws IOException {
        this.uploadDir = Path.of(properties.getLocalUploadDir()).toAbsolutePath();
        Files.createDirectories(uploadDir);
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
        String ext = switch (contentType) {
            case "image/png" -> ".png";
            case "image/gif" -> ".gif";
            case "image/webp" -> ".webp";
            default -> ".jpg";
        };
        String filename = folder + "/" + UUID.randomUUID() + ext;
        Path target = uploadDir.resolve(filename.replace("/", java.io.File.separator));
        try {
            Files.createDirectories(target.getParent());
            file.transferTo(target);
        } catch (IOException e) {
            throw new IllegalStateException("Upload failed", e);
        }
        return "/uploads/" + filename;
    }
}
