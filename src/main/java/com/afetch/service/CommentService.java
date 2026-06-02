package com.afetch.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.afetch.domain.entity.Post;
import com.afetch.domain.entity.PostComment;
import com.afetch.domain.entity.User;
import com.afetch.domain.enums.PostVisibility;
import com.afetch.repository.FriendshipRepository;
import com.afetch.repository.PostCommentRepository;
import com.afetch.repository.PostRepository;
import com.afetch.repository.UserRepository;
import com.afetch.util.MentionParser;
import com.afetch.web.dto.post.CommentResponse;
import com.afetch.web.dto.post.CreateCommentRequest;

@Service
public class CommentService {

    private final PostCommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final FriendshipRepository friendshipRepository;

    public CommentService(PostCommentRepository commentRepository,
                          PostRepository postRepository,
                          UserRepository userRepository,
                          FriendshipRepository friendshipRepository) {
        this.commentRepository = commentRepository;
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.friendshipRepository = friendshipRepository;
    }

    @Transactional
    public CommentResponse addComment(Long postId, Long userId, CreateCommentRequest request) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        ensurePostAccess(post, userId);

        User author = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        PostComment comment = new PostComment();
        comment.setPost(post);
        comment.setAuthor(author);
        comment.setBody(request.body());

        if (request.parentId() != null) {
            PostComment parent = commentRepository.findById(request.parentId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
            if (!parent.getPost().getId().equals(postId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Parent comment must belong to the same post");
            }
            comment.setParent(parent);
        }

        comment.getMentions().addAll(MentionParser.parse(request.body(), userRepository));
        commentRepository.save(comment);
        return toResponse(comment);
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> getComments(Long postId, Long viewerId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        ensurePostAccess(post, viewerId);

        return commentRepository.findByPostIdOrderByCreatedAtAsc(postId).stream()
                .map(this::toResponse)
                .toList();
    }

    private CommentResponse toResponse(PostComment comment) {
        List<String> mentions = comment.getMentions().stream().map(User::getUsername).toList();
        return new CommentResponse(
                comment.getId(),
                comment.getPost().getId(),
                comment.getAuthor().getId(),
                comment.getParent() != null ? comment.getParent().getId() : null,
                comment.getBody(),
                comment.getAuthor().getUsername(),
                comment.getAuthor().getAvatarUrl(),
                mentions,
                comment.getCreatedAt()
        );
    }

    private void ensurePostAccess(Post post, Long userId) {
        if (post.getVisibility() == PostVisibility.PUBLIC_FEED) {
            return;
        }
        if (post.getAuthor() != null && post.getAuthor().getId().equals(userId)) {
            return;
        }
        if (!friendshipRepository.areFriends(post.getAuthor().getId(), userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
    }
}
