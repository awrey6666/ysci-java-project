package com.afetch.repository;

import com.afetch.domain.entity.ChatRoom;
import com.afetch.domain.enums.ChatRoomType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    List<ChatRoom> findByType(ChatRoomType type);

    Optional<ChatRoom> findBySlug(String slug);

    @Query("SELECT r FROM ChatRoom r JOIN ChatRoomMember m ON m.roomId = r.id " +
           "WHERE r.type = com.afetch.domain.enums.ChatRoomType.DM " +
           "AND m.userId IN (:u1, :u2) GROUP BY r HAVING COUNT(DISTINCT m.userId) = 2")
    Optional<ChatRoom> findDmBetween(@Param("u1") Long u1, @Param("u2") Long u2);
}
