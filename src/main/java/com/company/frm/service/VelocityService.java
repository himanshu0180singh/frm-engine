package com.company.frm.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class VelocityService {

    private final StringRedisTemplate redisTemplate;

    @Value("${frm.velocity.redis-key-prefix:frm:velocity:}")
    private String keyPrefix;

    public long getTransactionCount(String customerId, int windowSeconds) {
        String key = keyPrefix + customerId + ":" + windowSeconds;
        try {
            String val = redisTemplate.opsForValue().get(key);
            return val == null ? 0L : Long.parseLong(val);
        } catch (Exception e) {
            log.warn("Redis error fetching velocity count for {}: {}", customerId, e.getMessage());
            return 0L;
        }
    }

    public void incrementTransactionCount(String customerId, int windowSeconds) {
        String key = keyPrefix + customerId + ":" + windowSeconds;
        try {
            Long count = redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1) {
                redisTemplate.expire(key, Duration.ofSeconds(windowSeconds));
            }
        } catch (Exception e) {
            log.warn("Redis error incrementing velocity for {}: {}", customerId, e.getMessage());
        }
    }
}
