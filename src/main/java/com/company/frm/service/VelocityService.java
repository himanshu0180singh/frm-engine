package com.company.frm.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class VelocityService {

    private final StringRedisTemplate redisTemplate;

    private static final String KEY_PREFIX_COUNT   = "vel:cnt:";
    private static final String KEY_PREFIX_AMOUNT  = "vel:amt:";
    private static final String KEY_PREFIX_CHANNEL = "vel:ch:";

    /**
     * Records a transaction and returns the count within the sliding window.
     */
    public long recordAndGetCount(String senderId, String txnId, int windowMinutes) {
        String key = KEY_PREFIX_COUNT + senderId;
        long nowMs = Instant.now().toEpochMilli();
        long windowMs = Duration.ofMinutes(windowMinutes).toMillis();

        redisTemplate.opsForZSet().add(key, txnId + ":" + nowMs, nowMs);
        redisTemplate.opsForZSet().removeRangeByScore(key, 0, nowMs - windowMs);
        redisTemplate.expire(key, Duration.ofMinutes(windowMinutes + 5));

        Long count = redisTemplate.opsForZSet().zCard(key);
        return count != null ? count : 0L;
    }

    /**
     * Returns cumulative amount for sender within the window (best-effort; Redis stores count only).
     * A full implementation would store amount as the score; here we return the count for simplicity.
     */
    public long getCountInWindow(String senderId, int windowMinutes) {
        String key = KEY_PREFIX_COUNT + senderId;
        long nowMs = Instant.now().toEpochMilli();
        long windowMs = Duration.ofMinutes(windowMinutes).toMillis();

        Set<String> members = redisTemplate.opsForZSet()
                .rangeByScore(key, nowMs - windowMs, nowMs);
        return members != null ? members.size() : 0L;
    }

    /**
     * Records the channel used by sender and returns the distinct channel count in the window.
     */
    public long recordAndGetChannelCount(String senderId, String channel, int windowMinutes) {
        String key = KEY_PREFIX_CHANNEL + senderId;
        long nowMs = Instant.now().toEpochMilli();
        long windowMs = Duration.ofMinutes(windowMinutes).toMillis();

        redisTemplate.opsForZSet().add(key, channel + ":" + nowMs, nowMs);
        redisTemplate.opsForZSet().removeRangeByScore(key, 0, nowMs - windowMs);
        redisTemplate.expire(key, Duration.ofMinutes(windowMinutes + 5));

        Set<String> members = redisTemplate.opsForZSet()
                .rangeByScore(key, nowMs - windowMs, nowMs);
        if (members == null) return 0L;
        return members.stream()
                .map(m -> m.contains(":") ? m.substring(0, m.lastIndexOf(':')) : m)
                .distinct()
                .count();
    }
}
