package com.company.frm.service;

import com.company.frm.enums.EntityType;
import com.company.frm.repository.BlacklistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class BlacklistService {

    private final BlacklistRepository blacklistRepository;

    @Cacheable(value = "blacklist", key = "#entityType + ':' + #entityValue")
    public boolean isBlacklisted(EntityType entityType, String entityValue) {
        LocalDateTime now = LocalDateTime.now();
        return blacklistRepository.existsByEntityTypeAndEntityValueAndIsActiveTrueAndExpiresAtIsNull(
                entityType, entityValue)
                || blacklistRepository.existsByEntityTypeAndEntityValueAndIsActiveTrueAndExpiresAtAfter(
                        entityType, entityValue, now);
    }
}
