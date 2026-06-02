package com.afetch.security;

import com.afetch.domain.entity.RefreshToken;
import com.afetch.repository.RefreshTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;

@Service
public class RefreshTokenValidator {

    private final RefreshTokenRepository refreshTokenRepository;

    public RefreshTokenValidator(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Transactional(readOnly = true)
    public Optional<UserPrincipal> authenticate(String rawRefreshToken) {
        return findValid(rawRefreshToken).map(token -> new UserPrincipal(token.getUser()));
    }

    @Transactional(readOnly = true)
    public Optional<RefreshToken> findValid(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return Optional.empty();
        }
        String hash = hashToken(rawRefreshToken);
        return refreshTokenRepository.findByTokenHashAndRevokedFalse(hash)
                .filter(token -> !token.getExpiresAt().isBefore(Instant.now()));
    }

    public String hashToken(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
