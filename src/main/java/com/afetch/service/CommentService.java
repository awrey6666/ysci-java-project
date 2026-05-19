package com.afetch.service;

import com.afetch.domain.entity.Post;
import com.afetch.domain.entity.PostComment;
import com.afetch.domain.entity.User;
import com.afetch.repository.PostCommentRepository;
import com.afetch.repository.PostRepository;
import com.afetch.repository.UserRepository;
import com.afetch.util.MentionParser;
import com.afetch.web.dto.post.CommentResponse;
import com.afetch.web.dto.post.CreateCommentRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class CommentService {

    private final PostCommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    public CommentService(PostCommentRepository commentRepository,
                          PostRepository postRepository,
                          UserRepository userRepository) {
        this.commentRepository = commentRepository;
        this.postRepository = postRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public CommentResponse addComment(Long postId, Long userId, CreateCommentRequest request) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        User author = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        PostComment comment = new PostComment();
        comment.setPost(post);
        comment.setAuthor(author);
        comment.setBody(request.body());

        if (request.parentId() != null) {
            PostComment parent = commentRepository.findById(request.parentId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
            comment.setParent(parent);
        }

        comment.getMentions().addAll(MentionParser.parse(request.body(), userRepository));
        commentRepository.save(comment);
        return toResponse(comment);
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> getComments(Long postId) {
        return commentRepository.findByPostIdOrderByCreatedAtAsc(postId).stream()
                .map(this::toResponse)
                .toList();
    }

    private CommentResponse toResponse(PostComment comment) {
        List<String> mentions = comment.getMentions().stream().map(User::getUsername).toList();
        return new CommentResponse(
                comment.getId(),
                comment.getPost().getId(),
                comment.getParent() != null ? comment.getParent().getId() : null,
                comment.getBody(),
                comment.getAuthor().getUsername(),
                mentions,
                comment.getCreatedAt()
        );
    }
}
