package com.afetch.repository;

import com.afetch.domain.entity.PostReaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PostReactionRepository extends JpaRepository<PostReaction, PostReaction.PostReactionId> {

    List<PostReaction> findByPostId(Long postId);

    boolean existsByPostIdAndUserId(Long postId, Long userId);

    void deleteByPostIdAndUserId(Long postId, Long userId);

    long countByPostId(Long postId);
}
