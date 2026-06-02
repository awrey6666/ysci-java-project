package com.afetch.service;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.afetch.domain.entity.ChatRoom;
import com.afetch.domain.entity.ChatRoomMember;
import com.afetch.domain.entity.User;
import com.afetch.domain.enums.ChatRoomType;
import com.afetch.domain.entity.ChatMessage;
import com.afetch.repository.ChatMessageRepository;
import com.afetch.repository.ChatRoomMemberRepository;
import com.afetch.repository.ChatRoomRepository;
import com.afetch.repository.UserRepository;
import com.afetch.web.dto.chat.ChatRoomResponse;

@Service
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository memberRepository;
    private final ChatMessageRepository messageRepository;
    private final UserRepository userRepository;
    private final ChatPresenceService presenceService;

    public ChatRoomService(ChatRoomRepository chatRoomRepository,
                           ChatRoomMemberRepository memberRepository,
                           ChatMessageRepository messageRepository,
                           UserRepository userRepository,
                           ChatPresenceService presenceService) {
        this.chatRoomRepository = chatRoomRepository;
        this.memberRepository = memberRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.presenceService = presenceService;
    }

    @Transactional(readOnly = true)
    public List<ChatRoomResponse> listRoomsForUser(Long userId) {
        return chatRoomRepository.findRoomsForUser(userId, ChatRoomType.BUILTIN).stream()
                .map(room -> toResponse(room, userId))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ChatRoomResponse> listBuiltinRooms() {
        return chatRoomRepository.findByType(ChatRoomType.BUILTIN).stream()
                .map(room -> toResponse(room, null))
                .toList();
    }

    @Transactional
    public ChatRoomResponse createDm(Long userId, Long otherUserId) {
        if (otherUserId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId is required");
        }
        if (userId.equals(otherUserId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        userRepository.findById(otherUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        ChatRoom existing = findExistingDm(userId, otherUserId);
        if (existing != null) {
            return toResponse(existing, userId);
        }

        User other = userRepository.findById(otherUserId).orElseThrow();
        User self = userRepository.findById(userId).orElseThrow();

        ChatRoom room = new ChatRoom();
        room.setType(ChatRoomType.DM);
        room.setName(self.getUsername() + " & " + other.getUsername());
        room.setCreatedBy(self);
        room.setLastActivity(Instant.now());
        chatRoomRepository.save(room);

        addMember(room.getId(), userId);
        addMember(room.getId(), otherUserId);
        return toResponse(room, userId);
    }

    @Transactional
    public ChatRoomResponse createGroup(Long creatorId, String name, List<Long> memberIds) {
        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        ChatRoom room = new ChatRoom();
        room.setType(ChatRoomType.GROUP);
        room.setName(name);
        room.setCreatedBy(creator);
        room.setLastActivity(Instant.now());
        chatRoomRepository.save(room);

        Set<Long> members = new HashSet<>(memberIds);
        members.add(creatorId);
        for (Long memberId : members) {
            if (!userRepository.existsById(memberId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Member not found: " + memberId);
            }
            addMember(room.getId(), memberId);
        }
        return toResponse(room, creatorId);
    }

    @Transactional
    public void markAsRead(Long roomId, Long userId) {
        ensureMember(roomId, userId);
        ChatRoomMember member = memberRepository.findByRoomIdAndUserId(roomId, userId)
                .orElseGet(() -> {
                    ChatRoomMember created = new ChatRoomMember();
                    created.setRoomId(roomId);
                    created.setUserId(userId);
                    return created;
                });
        member.setLastReadAt(Instant.now());
        memberRepository.save(member);
    }

    @Transactional
    public void joinRoom(Long roomId, Long userId) {
        ChatRoom room = ensureRoomExists(roomId);
        boolean isMember = memberRepository.existsByRoomIdAndUserId(roomId, userId);

        if (room.getType() == ChatRoomType.BUILTIN) {
            if (!isMember) {
                addMember(roomId, userId);
            }
        } else {
            if (!isMember) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not a member of this room");
            }
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

    private ChatRoomResponse toResponse(ChatRoom room, Long viewerId) {
        String preview = null;
        Instant lastAt = room.getLastActivity();
        ChatMessage latest = messageRepository.findFirstByRoom_IdOrderByCreatedAtDesc(room.getId()).orElse(null);
        if (latest != null) {
            lastAt = latest.getCreatedAt();
            preview = latest.getBody();
            if (preview != null && preview.length() > 80) {
                preview = preview.substring(0, 77) + "...";
            }
        }

        int unread = 0;
        Long dmPartnerId = null;
        String dmPartnerUsername = null;
        String dmPartnerDisplayName = null;
        String dmPartnerAvatarUrl = null;

        if (viewerId != null) {
            Instant since = memberRepository.findByRoomIdAndUserId(room.getId(), viewerId)
                    .map(ChatRoomMember::getLastReadAt)
                    .orElse(Instant.EPOCH);
            unread = (int) messageRepository.countUnreadSince(room.getId(), viewerId, since);

            if (room.getType() == ChatRoomType.DM) {
                for (ChatRoomMember member : memberRepository.findByRoomId(room.getId())) {
                    if (!member.getUserId().equals(viewerId)) {
                        User partner = userRepository.findById(member.getUserId()).orElse(null);
                        if (partner != null) {
                            dmPartnerId = partner.getId();
                            dmPartnerUsername = partner.getUsername();
                            dmPartnerDisplayName = partner.getDisplayName();
                            dmPartnerAvatarUrl = partner.getAvatarUrl();
                        }
                        break;
                    }
                }
            }
        }

        return new ChatRoomResponse(
                room.getId(),
                room.getType().name(),
                room.getSlug(),
                room.getName(),
                presenceService.onlineCount(room.getId()),
                preview,
                lastAt,
                unread,
                dmPartnerId,
                dmPartnerUsername,
                dmPartnerDisplayName,
                dmPartnerAvatarUrl
        );
    }
}
