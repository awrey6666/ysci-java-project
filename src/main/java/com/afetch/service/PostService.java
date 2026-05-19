package com.afetch.service;

import com.afetch.domain.entity.Post;
import com.afetch.domain.entity.PostImage;
import com.afetch.domain.entity.PostReaction;
import com.afetch.domain.entity.User;
import com.afetch.domain.enums.PostVisibility;
import com.afetch.domain.enums.ReactionType;
import com.afetch.repository.FriendshipRepository;
import com.afetch.repository.PostReactionRepository;
import com.afetch.repository.PostRepository;
import com.afetch.repository.UserRepository;
import com.afetch.web.dto.post.CreatePostRequest;
import com.afetch.web.dto.post.PostResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@Service
public class PostService {

    private final PostRepository postRepository;
    private final PostReactionRepository reactionRepository;
    private final FriendshipRepository friendshipRepository;
    private final UserRepository userRepository;

    public PostService(PostRepository postRepository,
                       PostReactionRepository reactionRepository,
                       FriendshipRepository friendshipRepository,
                       UserRepository userRepository) {
        this.postRepository = postRepository;
        this.reactionRepository = reactionRepository;
        this.friendshipRepository = friendshipRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public PostResponse createPost(Long userId, CreatePostRequest request) {
        User author = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        PostVisibility visibility = request.visibility() != null && request.visibility().equals("FRIENDS_ONLY")
                ? PostVisibility.FRIENDS_ONLY
                : PostVisibility.PUBLIC_FEED;

        Post post = new Post();
        post.setAuthor(author);
        post.setBody(request.body());
        post.setAnonymous(request.anonymous());
        post.setVisibility(visibility);

        if (request.imageUrls() != null) {
            int order = 0;
            for (String url : request.imageUrls()) {
                PostImage image = new PostImage();
                image.setPost(post);
                image.setUrl(url);
                image.setSortOrder(order++);
                post.getImages().add(image);
            }
        }

        postRepository.save(post);
        return toResponse(post, userId);
    }

    @Transactional(readOnly = true)
    public List<PostResponse> getPublicFeed(Long currentUserId) {
        return postRepository.findFeedPosts(PostVisibility.PUBLIC_FEED).stream()
                .map(p -> toResponse(p, currentUserId))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PostResponse> getProfilePosts(Long authorId, Long viewerId) {
        if (!authorId.equals(viewerId) && !friendshipRepository.areFriends(authorId, viewerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not friends with this user");
        }
        return postRepository.findFriendsOnlyByAuthor(authorId).stream()
                .map(p -> toResponse(p, viewerId))
                .toList();
    }

    @Transactional
    public void toggleLike(Long postId, Long userId) {
        if (!postRepository.existsById(postId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (reactionRepository.existsByPostIdAndUserId(postId, userId)) {
            reactionRepository.deleteByPostIdAndUserId(postId, userId);
        } else {
            PostReaction reaction = new PostReaction();
            reaction.setPostId(postId);
            reaction.setUserId(userId);
            reaction.setType(ReactionType.LIKE);
            reactionRepository.save(reaction);
        }
    }

    private PostResponse toResponse(Post post, Long currentUserId) {
        List<String> urls = post.getImages().stream().map(PostImage::getUrl).toList();
        long likes = reactionRepository.countByPostId(post.getId());
        boolean liked = reactionRepository.existsByPostIdAndUserId(post.getId(), currentUserId);

        String authorUsername = null;
        Long authorId = null;
        if (!post.isAnonymous() && post.getAuthor() != null) {
            authorUsername = post.getAuthor().getUsername();
            authorId = post.getAuthor().getId();
        }

        return new PostResponse(
                post.getId(),
                post.getBody(),
                post.isAnonymous(),
                post.getVisibility().name(),
                authorUsername,
                authorId,
                urls,
                likes,
                liked,
                post.getCreatedAt()
        );
    }
}
