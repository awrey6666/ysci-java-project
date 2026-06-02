package com.afetch.repository;

import com.afetch.domain.entity.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findByRoom_IdOrderByCreatedAtDesc(Long roomId, Pageable pageable);

    Optional<ChatMessage> findFirstByRoom_IdOrderByCreatedAtDesc(Long roomId);

    @Query("SELECT COUNT(m) FROM ChatMessage m WHERE m.room.id = :roomId " +
           "AND m.sender.id <> :userId AND m.createdAt > :since")
    long countUnreadSince(@Param("roomId") Long roomId,
                          @Param("userId") Long userId,
                          @Param("since") Instant since);
}
