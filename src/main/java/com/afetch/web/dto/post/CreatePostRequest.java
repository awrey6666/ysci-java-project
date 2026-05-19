package com.afetch.web.dto.post;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreatePostRequest(
        @NotBlank @Size(max = 5000) String body,
        boolean anonymous,
        String visibility,
        List<String> imageUrls
) {}
