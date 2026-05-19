package com.afetch.service;

import com.afetch.domain.entity.ChatRoom;
import com.afetch.domain.entity.ChatRoomMember;
import com.afetch.domain.entity.User;
import com.afetch.domain.enums.ChatRoomType;
import com.afetch.repository.ChatRoomMemberRepository;
import com.afetch.repository.ChatRoomRepository;
import com.afetch.repository.UserRepository;
import com.afetch.web.dto.chat.ChatRoomResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final ChatPresenceService presenceService;

    public ChatRoomService(ChatRoomRepository chatRoomRepository,
                           ChatRoomMemberRepository memberRepository,
                           UserRepository userRepository,
                           ChatPresenceService presenceService) {
        this.chatRoomRepository = chatRoomRepository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
        this.presenceService = presenceService;
    }

    @Transactional(readOnly = true)
    public List<ChatRoomResponse> listRoomsForUser(Long userId) {
        List<ChatRoom> builtin = chatRoomRepository.findByType(ChatRoomType.BUILTIN);
        List<ChatRoom> memberRooms = memberRepository.findByUserId(userId).stream()
                .map(m -> chatRoomRepository.findById(m.getRoomId()).orElseThrow())
                .filter(r -> r.getType() != ChatRoomType.BUILTIN)
                .toList();

        Set<Long> seen = new HashSet<>();
        return java.util.stream.Stream.concat(builtin.stream(), memberRooms.stream())
                .filter(r -> seen.add(r.getId()))
                .map(r -> toResponse(r))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ChatRoomResponse> listBuiltinRooms() {
        return chatRoomRepository.findByType(ChatRoomType.BUILTIN).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ChatRoomResponse createDm(Long userId, Long otherUserId) {
        if (userId.equals(otherUserId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        userRepository.findById(otherUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        ChatRoom existing = findExistingDm(userId, otherUserId);
        if (existing != null) {
            return toResponse(existing);
        }

        User other = userRepository.findById(otherUserId).orElseThrow();
        User self = userRepository.findById(userId).orElseThrow();

        ChatRoom room = new ChatRoom();
        room.setType(ChatRoomType.DM);
        room.setName(self.getUsername() + " & " + other.getUsername());
        room.setCreatedBy(self);
        chatRoomRepository.save(room);

        addMember(room.getId(), userId);
        addMember(room.getId(), otherUserId);
        return toResponse(room);
    }

    @Transactional
    public ChatRoomResponse createGroup(Long creatorId, String name, List<Long> memberIds) {
        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        ChatRoom room = new ChatRoom();
        room.setType(ChatRoomType.GROUP);
        room.setName(name);
        room.setCreatedBy(creator);
        chatRoomRepository.save(room);

        Set<Long> members = new HashSet<>(memberIds);
        members.add(creatorId);
        for (Long memberId : members) {
            addMember(room.getId(), memberId);
        }
        return toResponse(room);
    }

    @Transactional
    public void joinRoom(Long roomId, Long userId) {
        ensureRoomExists(roomId);
        if (!memberRepository.existsByRoomIdAndUserId(roomId, userId)) {
            addMember(roomId, userId);
        }
        presenceService.joinRoom(roomId, userId);
    }

    @Transactional(readOnly = true)
    public void ensureMember(Long roomId, Long userId) {
        ChatRoom room = ensureRoomExists(roomId);
        if (room.getType() == ChatRoomType.BUILTIN) {
            return;
        }
        if (!memberRepository.existsByRoomIdAndUserId(roomId, userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not a member of this room");
        }
    }

    private ChatRoom findExistingDm(Long u1, Long u2) {
        for (ChatRoomMember m : memberRepository.findByUserId(u1)) {
            ChatRoom room = chatRoomRepository.findById(m.getRoomId()).orElse(null);
            if (room != null && room.getType() == ChatRoomType.DM
                    && memberRepository.existsByRoomIdAndUserId(room.getId(), u2)) {
                return room;
            }
        }
        return null;
    }

    private void addMember(Long roomId, Long userId) {
        ChatRoomMember member = new ChatRoomMember();
        member.setRoomId(roomId);
        member.setUserId(userId);
        memberRepository.save(member);
    }

    private ChatRoom ensureRoomExists(Long roomId) {
        return chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private ChatRoomResponse toResponse(ChatRoom room) {
        return new ChatRoomResponse(
                room.getId(),
                room.getType().name(),
                room.getSlug(),
                room.getName(),
                presenceService.onlineCount(room.getId())
        );
    }
}
