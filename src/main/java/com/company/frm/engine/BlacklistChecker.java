package com.company.frm.engine;

import com.company.frm.dto.TransactionRequest;
import com.company.frm.enums.EntityType;
import com.company.frm.service.BlacklistService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BlacklistChecker {

    private final BlacklistService blacklistService;

    public boolean isBlacklisted(TransactionRequest request) {
        if (request.getCustomerId() != null
                && blacklistService.isBlacklisted(EntityType.CUSTOMER_ID, request.getCustomerId())) {
            return true;
        }
        if (request.getIpAddress() != null
                && blacklistService.isBlacklisted(EntityType.IP_ADDRESS, request.getIpAddress())) {
            return true;
        }
        if (request.getDeviceId() != null
                && blacklistService.isBlacklisted(EntityType.DEVICE_ID, request.getDeviceId())) {
            return true;
        }
        return false;
    }
}
