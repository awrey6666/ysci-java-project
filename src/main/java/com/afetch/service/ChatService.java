package com.afetch.service;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.afetch.domain.entity.ChatMessage;
import com.afetch.domain.entity.ChatRoom;
import com.afetch.domain.entity.User;
import com.afetch.repository.ChatMessageRepository;
import com.afetch.repository.ChatRoomRepository;
import com.afetch.repository.UserRepository;
import com.afetch.util.MentionParser;
import com.afetch.web.dto.chat.ChatMessageResponse;
import com.afetch.web.dto.chat.SendChatMessageRequest;

@Service
public class ChatService {

    private final ChatMessageRepository messageRepository;
    private final ChatRoomRepository roomRepository;
    private final UserRepository userRepository;
    private final ChatRoomService chatRoomService;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatService(ChatMessageRepository messageRepository,
                       ChatRoomRepository roomRepository,
                       UserRepository userRepository,
                       ChatRoomService chatRoomService,
                       SimpMessagingTemplate messagingTemplate) {
        this.messageRepository = messageRepository;
        this.roomRepository = roomRepository;
        this.userRepository = userRepository;
        this.chatRoomService = chatRoomService;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public ChatMessageResponse sendMessage(Long userId, SendChatMessageRequest request) {
        chatRoomService.ensureMember(request.roomId(), userId);

        ChatRoom room = roomRepository.findById(request.roomId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        User sender = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        ChatMessage message = new ChatMessage();
        message.setRoom(room);
        message.setSender(sender);
        message.setBody(request.body());
        message.setImageUrl(request.imageUrl());

        if (request.parentId() != null) {
            ChatMessage parent = messageRepository.findById(request.parentId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
            if (!parent.getRoom().getId().equals(request.roomId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Parent message must belong to the same room");
            }
            message.setParent(parent);
        }

        if (request.body() != null) {
            message.getMentions().addAll(MentionParser.parse(request.body(), userRepository));
        }

        room.setLastActivity(Instant.now());
        messageRepository.save(message);
        chatRoomService.markAsRead(request.roomId(), userId);
        ChatMessageResponse response = toResponse(message);
        messagingTemplate.convertAndSend("/topic/room." + request.roomId(), response);
        return response;
    }

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getHistory(Long roomId, Long userId, int limit) {
        chatRoomService.ensureMember(roomId, userId);
        return messageRepository.findByRoom_IdOrderByCreatedAtDesc(roomId, PageRequest.of(0, limit))
                .stream()
                .map(this::toResponse)
                .sorted((a, b) -> a.createdAt().compareTo(b.createdAt()))
                .toList();
    }

    private ChatMessageResponse toResponse(ChatMessage message) {
        List<String> mentions = message.getMentions().stream().map(User::getUsername).toList();
        return new ChatMessageResponse(
                message.getId(),
                message.getRoom().getId(),
                message.getParent() != null ? message.getParent().getId() : null,
                message.getBody(),
                message.getImageUrl(),
                message.getSender().getUsername(),
                message.getSender().getId(),
                message.getSender().getAvatarUrl(),
                mentions,
                message.getCreatedAt()
        );
    }
}
