package com.afetch.repository;

import com.afetch.domain.entity.AiConversation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AiConversationRepository extends JpaRepository<AiConversation, Long> {

    Optional<AiConversation> findTopByUserIdOrderByCreatedAtDesc(Long userId);
}
