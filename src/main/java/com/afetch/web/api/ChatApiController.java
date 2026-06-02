package com.afetch.web.api;

import com.afetch.security.SecurityUtils;
import com.afetch.service.ChatRoomService;
import com.afetch.service.ChatService;
import com.afetch.web.dto.chat.ChatMessageResponse;
import com.afetch.web.dto.chat.ChatRoomResponse;
import com.afetch.web.dto.chat.SendChatMessageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rooms")
public class ChatApiController {

    private final ChatRoomService chatRoomService;
    private final ChatService chatService;

    public ChatApiController(ChatRoomService chatRoomService, ChatService chatService) {
        this.chatRoomService = chatRoomService;
        this.chatService = chatService;
    }

    @GetMapping
    public List<ChatRoomResponse> rooms() {
        return chatRoomService.listRoomsForUser(SecurityUtils.currentUserId());
    }

    @GetMapping("/builtin")
    public List<ChatRoomResponse> builtin() {
        return chatRoomService.listBuiltinRooms();
    }

    @PostMapping("/{roomId}/join")
    public void join(@PathVariable Long roomId) {
        chatRoomService.joinRoom(roomId, SecurityUtils.currentUserId());
    }

    @GetMapping("/{roomId}/messages")
    public List<ChatMessageResponse> history(@PathVariable Long roomId,
                                             @RequestParam(defaultValue = "50") int limit) {
        return chatService.getHistory(roomId, SecurityUtils.currentUserId(), limit);
    }

    @PostMapping("/{roomId}/messages")
    public ChatMessageResponse sendMessage(@PathVariable Long roomId,
                                           @RequestBody SendChatMessageRequest request) {
        Long userId = SecurityUtils.currentUserId();
        SendChatMessageRequest payload = new SendChatMessageRequest(
                roomId,
                request.body(),
                request.imageUrl(),
                request.parentId()
        );
        return chatService.sendMessage(userId, payload);
    }

    @PostMapping("/{roomId}/read")
    public void markRead(@PathVariable Long roomId) {
        chatRoomService.markAsRead(roomId, SecurityUtils.currentUserId());
    }

    @PostMapping("/dm")
    public ChatRoomResponse createDm(@RequestBody Map<String, Long> body) {
        Long otherUserId = body.get("userId");
        if (otherUserId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId is required");
        }
        return chatRoomService.createDm(SecurityUtils.currentUserId(), otherUserId);
    }

    @PostMapping("/group")
    public ChatRoomResponse createGroup(@RequestBody Map<String, Object> body) {
        String name = body.get("name") instanceof String s ? s.trim() : null;
        if (!StringUtils.hasText(name)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Group name is required");
        }
        Object rawMembers = body.get("memberIds");
        if (!(rawMembers instanceof List<?> list) || list.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one member id is required");
        }
        List<Long> memberIds = list.stream()
                .filter(Number.class::isInstance)
                .map(n -> ((Number) n).longValue())
                .toList();
        if (memberIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "memberIds must contain numeric user ids");
        }
        return chatRoomService.createGroup(SecurityUtils.currentUserId(), name, memberIds);
    }
}
