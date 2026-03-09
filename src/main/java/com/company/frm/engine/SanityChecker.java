package com.company.frm.engine;

import com.company.frm.dto.FraudCheckResponse;
import com.company.frm.dto.RuleResult;
import com.company.frm.dto.TransactionRequest;
import com.company.frm.engine.rules.FraudRule;
import com.company.frm.engine.rules.RiskScoreAggregator;
import com.company.frm.enums.Decision;
import com.company.frm.enums.EntityType;
import com.company.frm.service.BlacklistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SanityChecker {

    private final BlacklistService blacklistService;
    private final RiskScoreAggregator aggregator;

    public FraudCheckResponse checkBlacklist(TransactionRequest request) {
        List<RuleResult> results = new ArrayList<>();
        boolean isBlacklisted = false;
        String reason = null;

        if (request.getCustomerId() != null
                && blacklistService.isBlacklisted(EntityType.CUSTOMER_ID, request.getCustomerId())) {
            isBlacklisted = true;
            reason = "Customer ID is blacklisted: " + request.getCustomerId();
        } else if (request.getIpAddress() != null
                && blacklistService.isBlacklisted(EntityType.IP_ADDRESS, request.getIpAddress())) {
            isBlacklisted = true;
            reason = "IP address is blacklisted: " + request.getIpAddress();
        } else if (request.getDeviceId() != null
                && blacklistService.isBlacklisted(EntityType.DEVICE_ID, request.getDeviceId())) {
            isBlacklisted = true;
            reason = "Device ID is blacklisted: " + request.getDeviceId();
        }

        if (isBlacklisted) {
            results.add(RuleResult.builder()
                    .ruleCode("ID_BLACKLIST")
                    .ruleName("Blacklisted Entity")
                    .fired(true)
                    .score(700)
                    .reason(reason)
                    .blocking(true)
                    .build());
            return FraudCheckResponse.builder()
                    .transactionId(request.getTransactionId())
                    .customerId(request.getCustomerId())
                    .decision(Decision.BLOCK)
                    .totalScore(700)
                    .decisionReason(reason)
                    .ruleResults(results)
                    .build();
        }
        return null;
    }
}
