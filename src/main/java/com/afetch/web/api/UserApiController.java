package com.afetch.web.api;

import com.afetch.security.SecurityUtils;
import com.afetch.service.UserService;
import com.afetch.web.dto.user.UpdateProfileRequest;
import com.afetch.web.dto.user.UserProfileResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserApiController {

    private final UserService userService;

    public UserApiController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public UserProfileResponse me() {
        return userService.getMyProfile(SecurityUtils.currentUserId());
    }

    @PutMapping("/me")
    public UserProfileResponse update(@Valid @RequestBody UpdateProfileRequest request) {
        return userService.updateProfile(SecurityUtils.currentUserId(), request);
    }

    @GetMapping("/search")
    public List<UserProfileResponse> search(@RequestParam String q) {
        return userService.search(q, SecurityUtils.currentUserId());
    }

    @GetMapping("/{username}")
    public UserProfileResponse profile(@PathVariable String username) {
        return userService.getProfile(username, SecurityUtils.currentUserId());
    }
}
