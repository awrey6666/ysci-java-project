package com.afetch.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.Set;

@Service
public class ChatPresenceService {

    private static final Logger log = LoggerFactory.getLogger(ChatPresenceService.class);
    private static final Duration PRESENCE_TTL = Duration.ofMinutes(5);

    private final StringRedisTemplate redis;

    public ChatPresenceService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public void joinRoom(Long roomId, Long userId) {
        try {
            String key = roomKey(roomId);
            redis.opsForSet().add(key, String.valueOf(userId));
            redis.expire(key, PRESENCE_TTL);
        } catch (Exception e) {
            log.warn("Redis unavailable for joinRoom room={} user={}", roomId, userId);
        }
    }

    public void leaveRoom(Long roomId, Long userId) {
        try {
            redis.opsForSet().remove(roomKey(roomId), String.valueOf(userId));
        } catch (Exception e) {
            log.warn("Redis unavailable for leaveRoom room={} user={}", roomId, userId);
        }
    }

    public void heartbeat(Long roomId, Long userId) {
        joinRoom(roomId, userId);
    }

    public long onlineCount(Long roomId) {
        try {
            Long size = redis.opsForSet().size(roomKey(roomId));
            return size != null ? size : 0;
        } catch (Exception e) {
            log.warn("Redis unavailable for onlineCount room={}", roomId);
            return 0;
        }
    }

    public Set<String> onlineUsers(Long roomId) {
        try {
            Set<String> members = redis.opsForSet().members(roomKey(roomId));
            return members != null ? members : Collections.emptySet();
        } catch (Exception e) {
            log.warn("Redis unavailable for onlineUsers room={}", roomId);
            return Collections.emptySet();
        }
    }

    private String roomKey(Long roomId) {
        return "room:" + roomId + ":online";
    }
}
