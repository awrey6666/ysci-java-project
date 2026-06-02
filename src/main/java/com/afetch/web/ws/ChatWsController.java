package com.afetch.web.ws;

import com.afetch.security.UserPrincipal;
import com.afetch.service.ChatPresenceService;
import com.afetch.service.ChatRoomService;
import com.afetch.service.ChatService;
import com.afetch.web.dto.chat.SendChatMessageRequest;
import org.springframework.http.HttpStatus;
import java.util.Map;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
public class ChatWsController {

    private final ChatService chatService;
    private final ChatRoomService chatRoomService;
    private final ChatPresenceService presenceService;

    public ChatWsController(ChatService chatService,
                            ChatRoomService chatRoomService,
                            ChatPresenceService presenceService) {
        this.chatService = chatService;
        this.chatRoomService = chatRoomService;
        this.presenceService = presenceService;
    }

    @MessageMapping("/chat.send")
    public void send(@Payload SendChatMessageRequest request, Principal principal) {
        UserPrincipal user = requireUser(principal);
        chatService.sendMessage(user.getId(), request);
    }

    @MessageMapping("/chat.join")
    public void join(@Payload Map<String, Object> payload, Principal principal) {
        UserPrincipal user = requireUser(principal);
        Object roomIdValue = payload.get("roomId");
        if (!(roomIdValue instanceof Number)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "roomId is required");
        }
        Long roomId = ((Number) roomIdValue).longValue();
        chatRoomService.joinRoom(roomId, user.getId());
    }

    @MessageMapping("/chat.leave")
    public void leave(@Payload Map<String, Object> payload, Principal principal) {
        if (principal == null) {
            return;
        }
        UserPrincipal user = requireUser(principal);
        Object roomIdValue = payload.get("roomId");
        if (!(roomIdValue instanceof Number)) {
            return;
        }
        Long roomId = ((Number) roomIdValue).longValue();
        presenceService.leaveRoom(roomId, user.getId());
    }

    private static UserPrincipal requireUser(Principal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "WebSocket not authenticated");
        }
        if (principal instanceof UserPrincipal userPrincipal) {
            return userPrincipal;
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid principal");
    }
}
