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
                .map(u -> toProfile(u, viewerId))
                .toList();
    }

    private UserProfileResponse toProfile(User user, Long viewerId) {
        boolean isSelf = user.getId().equals(viewerId);
        boolean isFriend = isSelf || friendshipRepository.areFriends(user.getId(), viewerId);
        boolean pending = friendshipRepository.findBetween(viewerId, user.getId())
                .map(f -> f.getStatus() == FriendshipStatus.PENDING)
                .orElse(false);

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
                pending
        );
    }
}
