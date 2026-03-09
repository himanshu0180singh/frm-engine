package com.company.frm.engine.checker;

import com.company.frm.dto.TransactionRequest;
import com.company.frm.service.BlacklistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class WhitelistChecker {

    private final BlacklistService blacklistService;

    /**
     * Returns true if sender is on the whitelist (trusted entity, skip further checks).
     */
    public boolean isSenderWhitelisted(TransactionRequest request) {
        try {
            return blacklistService.isBlacklisted("WHITELIST_ACCOUNT", request.getSenderId());
        } catch (Exception e) {
            log.warn("Whitelist check failed: {}", e.getMessage());
            return false;
        }
    }
}
