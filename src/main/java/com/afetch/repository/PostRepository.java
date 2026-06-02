package com.afetch.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.afetch.domain.entity.Post;
import com.afetch.domain.enums.PostVisibility;

public interface PostRepository extends JpaRepository<Post, Long> {

    @Query("SELECT DISTINCT p FROM Post p LEFT JOIN FETCH p.images " +
           "WHERE p.visibility = :visibility ORDER BY p.createdAt DESC")
    List<Post> findFeedPosts(@Param("visibility") PostVisibility visibility);

    @Query("SELECT DISTINCT p FROM Post p LEFT JOIN FETCH p.images " +
           "WHERE p.visibility = :publicVisibility OR " +
           "(p.visibility = :friendsVisibility AND p.author.id IN :authorIds) " +
           "ORDER BY p.createdAt DESC")
    List<Post> findFeedPosts(@Param("publicVisibility") PostVisibility publicVisibility,
                             @Param("friendsVisibility") PostVisibility friendsVisibility,
                             @Param("authorIds") List<Long> authorIds);

    @Query("SELECT DISTINCT p FROM Post p LEFT JOIN FETCH p.images " +
           "WHERE p.author.id = :authorId AND p.visibility IN :visibilities " +
           "ORDER BY p.createdAt DESC")
    List<Post> findByAuthorIdAndVisibilityIn(@Param("authorId") Long authorId,
                                              @Param("visibilities") List<PostVisibility> visibilities);

    @Query("SELECT DISTINCT p FROM Post p LEFT JOIN FETCH p.images WHERE p.author.id = :authorId " +
           "AND p.visibility = com.afetch.domain.enums.PostVisibility.FRIENDS_ONLY ORDER BY p.createdAt DESC")
    List<Post> findFriendsOnlyByAuthor(@Param("authorId") Long authorId);
}
