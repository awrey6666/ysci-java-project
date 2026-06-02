package com.afetch.web.api;

import com.afetch.security.SecurityUtils;
import com.afetch.service.FriendshipService;
import com.afetch.web.dto.user.FriendshipResponse;
import com.afetch.web.dto.user.UserProfileResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/friends")
public class FriendApiController {

    private final FriendshipService friendshipService;

    public FriendApiController(FriendshipService friendshipService) {
        this.friendshipService = friendshipService;
    }

    @PostMapping("/request")
    public void request(@RequestBody Map<String, Long> body) {
        friendshipService.sendRequest(SecurityUtils.currentUserId(), body.get("userId"));
    }

    @PostMapping("/{id}/accept")
    public UserProfileResponse accept(@PathVariable Long id) {
        return friendshipService.accept(SecurityUtils.currentUserId(), id);
    }

    @PostMapping("/{id}/reject")
    public void reject(@PathVariable Long id) {
        friendshipService.reject(SecurityUtils.currentUserId(), id);
    }

    @GetMapping
    public List<UserProfileResponse> friends() {
        return friendshipService.listFriends(SecurityUtils.currentUserId());
    }

    @GetMapping("/pending")
    public List<FriendshipResponse> pending() {
        return friendshipService.listPending(SecurityUtils.currentUserId());
    }

    @GetMapping("/pending/sent")
    public List<FriendshipResponse> pendingSent() {
        return friendshipService.listPendingOutgoing(SecurityUtils.currentUserId());
    }

    @GetMapping("/user/{userId}")
    public List<UserProfileResponse> userFriends(@PathVariable Long userId) {
        return friendshipService.listFriendsForViewer(userId, SecurityUtils.currentUserId());
    }
}
