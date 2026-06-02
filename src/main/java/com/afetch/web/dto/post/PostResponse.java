package com.afetch.web.dto.post;

import java.time.Instant;
import java.util.List;

public record PostResponse(
        Long id,
        String body,
        boolean anonymous,
        String visibility,
        String authorUsername,
        Long authorId,
        String authorAvatarUrl,
        List<String> imageUrls,
        long likeCount,
        boolean likedByCurrentUser,
        Instant createdAt
) {}
