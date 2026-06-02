package com.afetch.web.dto.post;

import java.time.Instant;
import java.util.List;

public record CommentResponse(
        Long id,
        Long postId,
        Long authorId,
        Long parentId,
        String body,
        String authorUsername,
        String authorAvatarUrl,
        List<String> mentionedUsernames,
        Instant createdAt
) {}
