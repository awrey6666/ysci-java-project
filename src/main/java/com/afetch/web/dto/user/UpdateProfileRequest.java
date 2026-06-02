package com.afetch.web.dto.user;

import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(min = 3, max = 50) String username,
        @Size(max = 100) String displayName,
        @Size(max = 1000) String bio,
        @Size(max = 512) String avatarUrl
) {}
