package com.afetch.repository;

import com.afetch.domain.entity.ChatRoomMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChatRoomMemberRepository extends JpaRepository<ChatRoomMember, ChatRoomMember.ChatRoomMemberId> {

    boolean existsByRoomIdAndUserId(Long roomId, Long userId);

    Optional<ChatRoomMember> findByRoomIdAndUserId(Long roomId, Long userId);

    List<ChatRoomMember> findByUserId(Long userId);

    List<ChatRoomMember> findByRoomId(Long roomId);
}
