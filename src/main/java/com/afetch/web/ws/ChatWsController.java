package com.afetch.web.ws;

import com.afetch.security.UserPrincipal;
import com.afetch.service.ChatPresenceService;
import com.afetch.service.ChatRoomService;
import com.afetch.service.ChatService;
import com.afetch.web.dto.chat.SendChatMessageRequest;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

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
    public void send(@Payload SendChatMessageRequest request,
                    @AuthenticationPrincipal UserPrincipal principal) {
        chatService.sendMessage(principal.getId(), request);
    }

    @SubscribeMapping("/room.{roomId}")
    public void subscribe(@DestinationVariable Long roomId,
                          @AuthenticationPrincipal UserPrincipal principal) {
        chatRoomService.joinRoom(roomId, principal.getId());
        presenceService.joinRoom(roomId, principal.getId());
    }

    @MessageMapping("/chat.leave")
    public void leave(@Payload Long roomId, @AuthenticationPrincipal UserPrincipal principal) {
        presenceService.leaveRoom(roomId, principal.getId());
    }
}
