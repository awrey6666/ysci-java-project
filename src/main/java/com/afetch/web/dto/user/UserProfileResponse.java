package com.afetch.web.dto.user;

import java.time.Instant;

public record UserProfileResponse(
        Long id,
        String username,
        String email,
        String displayName,
        String bio,
        String avatarUrl,
        Instant createdAt,
        boolean isSelf,
        boolean isFriend,
        boolean hasPendingRequest
) {}
