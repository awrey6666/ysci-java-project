package com.afetch.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.afetch.domain.entity.ChatRoom;
import com.afetch.domain.enums.ChatRoomType;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    List<ChatRoom> findByType(ChatRoomType type);

    Optional<ChatRoom> findBySlug(String slug);

    @Query("SELECT DISTINCT r FROM ChatRoom r LEFT JOIN ChatRoomMember m ON m.roomId = r.id " +
           "WHERE r.type = :builtinType OR m.userId = :userId " +
           "ORDER BY COALESCE(r.lastActivity, r.createdAt) DESC")
    List<ChatRoom> findRoomsForUser(@Param("userId") Long userId, @Param("builtinType") ChatRoomType builtinType);

    @Query("SELECT r FROM ChatRoom r JOIN ChatRoomMember m ON m.roomId = r.id " +
           "WHERE r.type = com.afetch.domain.enums.ChatRoomType.DM " +
           "AND m.userId IN (:u1, :u2) GROUP BY r HAVING COUNT(DISTINCT m.userId) = 2")
    Optional<ChatRoom> findDmBetween(@Param("u1") Long u1, @Param("u2") Long u2);
}
