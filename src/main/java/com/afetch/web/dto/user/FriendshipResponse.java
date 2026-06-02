package com.afetch.web.dto.user;

import java.time.Instant;

public record FriendshipResponse(
        Long id,
        String requesterUsername,
        Long requesterId,
        String addresseeUsername,
        Long addresseeId,
        Instant createdAt
) {}
