package com.company.frm.service;

import com.company.frm.entity.FrmBlacklist;
import com.company.frm.repository.FrmBlacklistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class BlacklistService {

    private final FrmBlacklistRepository blacklistRepository;

    @Cacheable(value = "blacklist", key = "#entityType + ':' + #entityValue")
    public boolean isBlacklisted(String entityType, String entityValue) {
        if (entityValue == null || entityValue.isBlank()) {
            return false;
        }
        return blacklistRepository
                .findByEntityTypeAndEntityValueAndIsActiveTrue(entityType, entityValue)
                .isPresent();
    }
}
