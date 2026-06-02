package com.afetch.web.dto.auth;

public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresInMinutes,
        Long userId,
        String username
) {
    public static AuthResponse of(String token, int minutes, Long userId, String username) {
        return new AuthResponse(token, "Bearer", minutes, userId, username);
    }
}
