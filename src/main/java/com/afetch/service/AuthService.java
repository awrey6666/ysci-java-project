package com.afetch.service;

import com.afetch.config.AfetchProperties;
import com.afetch.domain.entity.RefreshToken;
import com.afetch.domain.entity.User;
import com.afetch.domain.enums.UserRole;
import com.afetch.repository.RefreshTokenRepository;
import com.afetch.repository.UserRepository;
import com.afetch.security.JwtTokenProvider;
import com.afetch.security.RefreshTokenValidator;
import com.afetch.security.UserPrincipal;
import com.afetch.web.dto.auth.AuthResponse;
import com.afetch.web.dto.auth.LoginRequest;
import com.afetch.web.dto.auth.RegisterRequest;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenValidator refreshTokenValidator;
    private final AfetchProperties properties;

    public AuthService(UserRepository userRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider jwtTokenProvider,
                       RefreshTokenValidator refreshTokenValidator,
                       AfetchProperties properties) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.refreshTokenValidator = refreshTokenValidator;
        this.properties = properties;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request, HttpServletResponse response) {
        if (userRepository.existsByUsername(request.username())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already taken");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }

        User user = new User();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(UserRole.USER);
        user.setDisplayName(request.username());
        userRepository.save(user);

        return issueTokens(new UserPrincipal(user), response);
    }

    @Transactional
    public AuthResponse login(LoginRequest request, HttpServletResponse response) {
        User user = userRepository.findByUsername(request.login())
                .or(() -> userRepository.findByEmail(request.login()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        return issueTokens(new UserPrincipal(user), response);
    }

    @Transactional
    public AuthResponse refresh(String rawRefreshToken, HttpServletResponse response) {
        RefreshToken token = refreshTokenValidator.findValid(rawRefreshToken)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));

        token.setRevoked(true);
        refreshTokenRepository.save(token);

        return issueTokens(new UserPrincipal(token.getUser()), response);
    }

    @Transactional
    public void logout(Long userId, HttpServletResponse response) {
        refreshTokenRepository.revokeAllForUser(userId);
        clearRefreshCookie(response);
    }

    private AuthResponse issueTokens(UserPrincipal principal, HttpServletResponse response) {
        String accessToken = jwtTokenProvider.createAccessToken(principal);
        String rawRefresh = UUID.randomUUID().toString();

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(userRepository.getReferenceById(principal.getId()));
        refreshToken.setTokenHash(refreshTokenValidator.hashToken(rawRefresh));
        refreshToken.setExpiresAt(Instant.now().plusSeconds(properties.getJwt().getRefreshExpirationDays() * 86400L));
        refreshTokenRepository.save(refreshToken);

        setRefreshCookie(response, rawRefresh);

        return AuthResponse.of(
                accessToken,
                properties.getJwt().getAccessExpirationMinutes(),
                principal.getId(),
                principal.getUsername()
        );
    }

    private void setRefreshCookie(HttpServletResponse response, String rawToken) {
        Cookie cookie = new Cookie(properties.getRefreshCookieName(), rawToken);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(properties.getJwt().getRefreshExpirationDays() * 86400);
        response.addCookie(cookie);
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(properties.getRefreshCookieName(), "");
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

}
