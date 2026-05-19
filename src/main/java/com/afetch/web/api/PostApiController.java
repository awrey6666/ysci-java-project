package com.afetch.web.api;

import com.afetch.security.SecurityUtils;
import com.afetch.service.CommentService;
import com.afetch.service.PostService;
import com.afetch.web.dto.post.CommentResponse;
import com.afetch.web.dto.post.CreateCommentRequest;
import com.afetch.web.dto.post.CreatePostRequest;
import com.afetch.web.dto.post.PostResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/posts")
public class PostApiController {

    private final PostService postService;
    private final CommentService commentService;

    public PostApiController(PostService postService, CommentService commentService) {
        this.postService = postService;
        this.commentService = commentService;
    }

    @GetMapping
    public List<PostResponse> feed() {
        return postService.getPublicFeed(SecurityUtils.currentUserId());
    }

    @PostMapping
    public PostResponse create(@Valid @RequestBody CreatePostRequest request) {
        return postService.createPost(SecurityUtils.currentUserId(), request);
    }

    @PostMapping("/{id}/reactions")
    public void toggleLike(@PathVariable Long id) {
        postService.toggleLike(id, SecurityUtils.currentUserId());
    }

    @GetMapping("/{id}/comments")
    public List<CommentResponse> comments(@PathVariable Long id) {
        return commentService.getComments(id);
    }

    @PostMapping("/{id}/comments")
    public CommentResponse addComment(@PathVariable Long id, @Valid @RequestBody CreateCommentRequest request) {
        return commentService.addComment(id, SecurityUtils.currentUserId(), request);
    }

    @GetMapping("/profile/{userId}")
    public List<PostResponse> profilePosts(@PathVariable Long userId) {
        return postService.getProfilePosts(userId, SecurityUtils.currentUserId());
    }
}
