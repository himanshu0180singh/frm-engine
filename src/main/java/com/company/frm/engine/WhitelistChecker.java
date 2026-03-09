package com.company.frm.engine;

import com.company.frm.dto.TransactionRequest;
import com.company.frm.enums.EntityType;
import com.company.frm.repository.WhitelistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class WhitelistChecker {

    private final WhitelistRepository whitelistRepository;

    public boolean isWhitelisted(TransactionRequest request) {
        if (request.getCustomerId() != null && isEntityWhitelisted(
                EntityType.CUSTOMER_ID, request.getCustomerId())) {
            return true;
        }
        return false;
    }

    private boolean isEntityWhitelisted(EntityType entityType, String entityValue) {
        LocalDateTime now = LocalDateTime.now();
        return whitelistRepository.existsByEntityTypeAndEntityValueAndIsActiveTrueAndExpiresAtIsNull(
                entityType, entityValue)
                || whitelistRepository.existsByEntityTypeAndEntityValueAndIsActiveTrueAndExpiresAtAfter(
                        entityType, entityValue, now);
    }
}
