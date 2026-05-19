package com.afetch.repository;

import com.afetch.domain.entity.Post;
import com.afetch.domain.enums.PostVisibility;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {

    @Query("SELECT p FROM Post p LEFT JOIN FETCH p.images WHERE p.visibility = :visibility ORDER BY p.createdAt DESC")
    List<Post> findFeedPosts(@Param("visibility") PostVisibility visibility);

    @Query("SELECT p FROM Post p LEFT JOIN FETCH p.images WHERE p.author.id = :authorId " +
           "AND p.visibility = com.afetch.domain.enums.PostVisibility.FRIENDS_ONLY ORDER BY p.createdAt DESC")
    List<Post> findFriendsOnlyByAuthor(@Param("authorId") Long authorId);
}
