package com.afetch.service;

import com.afetch.domain.entity.User;
import com.afetch.domain.enums.FriendshipStatus;
import com.afetch.repository.FriendshipRepository;
import com.afetch.repository.UserRepository;
import com.afetch.web.dto.user.UpdateProfileRequest;
import com.afetch.web.dto.user.UserProfileResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final FriendshipRepository friendshipRepository;

    public UserService(UserRepository userRepository, FriendshipRepository friendshipRepository) {
        this.userRepository = userRepository;
        this.friendshipRepository = friendshipRepository;
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(String username, Long viewerId) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return toProfile(user, viewerId);
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getMyProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return toProfile(user, userId);
    }

    @Transactional
    public UserProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (request.username() != null) {
            String username = request.username().trim();
            if (username.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username cannot be empty");
            }
            userRepository.findByUsername(username)
                    .filter(existing -> !existing.getId().equals(userId))
                    .ifPresent(existing -> {
                        throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already taken");
                    });
            user.setUsername(username);
        }
        if (request.displayName() != null) {
            user.setDisplayName(request.displayName());
        }
        if (request.bio() != null) {
            user.setBio(request.bio());
        }
        if (request.avatarUrl() != null) {
            user.setAvatarUrl(request.avatarUrl());
        }
        userRepository.save(user);
        return toProfile(user, userId);
    }

    @Transactional(readOnly = true)
    public List<UserProfileResponse> search(String query, Long viewerId) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        return userRepository.search(query.trim()).stream()
                .filter(u -> !u.getId().equals(viewerId))
                .map(u -> toProfile(u, viewerId))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<UserProfileResponse> browse(Long viewerId) {
        return userRepository.findByIdNotOrderByCreatedAtDesc(viewerId).stream()
                .limit(50)
                .map(u -> toProfile(u, viewerId))
                .toList();
    }

    private UserProfileResponse toProfile(User user, Long viewerId) {
        boolean isSelf = user.getId().equals(viewerId);
        boolean isFriend = isSelf || friendshipRepository.areFriends(user.getId(), viewerId);
        var friendship = friendshipRepository.findBetween(viewerId, user.getId());
        boolean pendingIncoming = friendship
                .filter(f -> f.getStatus() == FriendshipStatus.PENDING)
                .map(f -> f.getAddressee().getId().equals(viewerId))
                .orElse(false);
        boolean requestSent = friendship
                .filter(f -> f.getStatus() == FriendshipStatus.PENDING)
                .map(f -> f.getRequester().getId().equals(viewerId))
                .orElse(false);

        int friendsCount = friendshipRepository.findAcceptedForUser(user.getId()).size();

        return new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                isSelf ? user.getEmail() : null,
                user.getDisplayName(),
                user.getBio(),
                user.getAvatarUrl(),
                user.getCreatedAt(),
                isSelf,
                isFriend,
                pendingIncoming,
                requestSent,
                friendsCount
        );
    }
}
