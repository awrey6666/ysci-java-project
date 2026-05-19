package com.afetch.web.api;

import com.afetch.security.SecurityUtils;
import com.afetch.service.ChatRoomService;
import com.afetch.service.ChatService;
import com.afetch.web.dto.chat.ChatMessageResponse;
import com.afetch.web.dto.chat.ChatRoomResponse;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping("/dm")
    public ChatRoomResponse createDm(@RequestBody Map<String, Long> body) {
        return chatRoomService.createDm(SecurityUtils.currentUserId(), body.get("userId"));
    }

    @PostMapping("/group")
    public ChatRoomResponse createGroup(@RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        @SuppressWarnings("unchecked")
        List<Long> memberIds = ((List<Number>) body.get("memberIds")).stream()
                .map(Number::longValue)
                .toList();
        return chatRoomService.createGroup(SecurityUtils.currentUserId(), name, memberIds);
    }
}
