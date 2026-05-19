package com.afetch.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;

@Service
public class ChatPresenceService {

    private static final Duration PRESENCE_TTL = Duration.ofMinutes(5);

    private final StringRedisTemplate redis;

    public ChatPresenceService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public void joinRoom(Long roomId, Long userId) {
        String key = roomKey(roomId);
        redis.opsForSet().add(key, String.valueOf(userId));
        redis.expire(key, PRESENCE_TTL);
    }

    public void leaveRoom(Long roomId, Long userId) {
        redis.opsForSet().remove(roomKey(roomId), String.valueOf(userId));
    }

    public void heartbeat(Long roomId, Long userId) {
        joinRoom(roomId, userId);
    }

    public long onlineCount(Long roomId) {
        Long size = redis.opsForSet().size(roomKey(roomId));
        return size != null ? size : 0;
    }

    public Set<String> onlineUsers(Long roomId) {
        return redis.opsForSet().members(roomKey(roomId));
    }

    private String roomKey(Long roomId) {
        return "room:" + roomId + ":online";
    }
}
