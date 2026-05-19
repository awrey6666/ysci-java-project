package com.afetch.service;

import com.afetch.domain.entity.Friendship;
import com.afetch.domain.entity.User;
import com.afetch.domain.enums.FriendshipStatus;
import com.afetch.repository.FriendshipRepository;
import com.afetch.repository.UserRepository;
import com.afetch.web.dto.user.FriendshipResponse;
import com.afetch.web.dto.user.UserProfileResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@Service
public class FriendshipService {

    private final FriendshipRepository friendshipRepository;
    private final UserRepository userRepository;
    private final UserService userService;

    public FriendshipService(FriendshipRepository friendshipRepository,
                             UserRepository userRepository,
                             UserService userService) {
        this.friendshipRepository = friendshipRepository;
        this.userRepository = userRepository;
        this.userService = userService;
    }

    @Transactional
    public void sendRequest(Long requesterId, Long addresseeId) {
        if (requesterId.equals(addresseeId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot friend yourself");
        }
        userRepository.findById(addresseeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (friendshipRepository.areFriends(requesterId, addresseeId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Already friends");
        }

        friendshipRepository.findBetween(requesterId, addresseeId).ifPresent(f -> {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Friend request already exists");
        });

        Friendship friendship = new Friendship();
        friendship.setRequester(userRepository.getReferenceById(requesterId));
        friendship.setAddressee(userRepository.getReferenceById(addresseeId));
        friendship.setStatus(FriendshipStatus.PENDING);
        friendshipRepository.save(friendship);
    }

    @Transactional
    public void accept(Long userId, Long friendshipId) {
        Friendship friendship = getPendingForAddressee(friendshipId, userId);
        friendship.setStatus(FriendshipStatus.ACCEPTED);
        friendshipRepository.save(friendship);
    }

    @Transactional
    public void reject(Long userId, Long friendshipId) {
        Friendship friendship = getPendingForAddressee(friendshipId, userId);
        friendship.setStatus(FriendshipStatus.REJECTED);
        friendshipRepository.save(friendship);
    }

    @Transactional(readOnly = true)
    public List<UserProfileResponse> listFriends(Long userId) {
        List<UserProfileResponse> friends = new ArrayList<>();
        for (Friendship f : friendshipRepository.findAcceptedForUser(userId)) {
            User other = f.getRequester().getId().equals(userId) ? f.getAddressee() : f.getRequester();
            friends.add(userService.getProfile(other.getUsername(), userId));
        }
        return friends;
    }

    @Transactional(readOnly = true)
    public List<FriendshipResponse> listPending(Long userId) {
        return friendshipRepository.findPendingForUser(userId, FriendshipStatus.PENDING).stream()
                .map(f -> new FriendshipResponse(
                        f.getId(),
                        f.getRequester().getUsername(),
                        f.getRequester().getId(),
                        f.getCreatedAt()))
                .toList();
    }

    private Friendship getPendingForAddressee(Long friendshipId, Long userId) {
        Friendship friendship = friendshipRepository.findById(friendshipId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!friendship.getAddressee().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        if (friendship.getStatus() != FriendshipStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request is not pending");
        }
        return friendship;
    }
}
