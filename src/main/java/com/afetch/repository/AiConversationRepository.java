package com.afetch.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.afetch.domain.entity.AiConversation;

public interface AiConversationRepository extends JpaRepository<AiConversation, Long> {

    Optional<AiConversation> findTopByUserIdOrderByCreatedAtDesc(Long userId);
    List<AiConversation> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<AiConversation> findByUserIdOrderByLastActivityDesc(Long userId);
    Optional<AiConversation> findByIdAndUserId(Long id, Long userId);
}
