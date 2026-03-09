package com.company.frm.engine.checker;

import com.company.frm.dto.RuleResult;
import com.company.frm.dto.TransactionRequest;
import com.company.frm.service.BlacklistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class BlacklistChecker {

    private final BlacklistService blacklistService;

    public List<RuleResult> check(TransactionRequest request) {
        List<RuleResult> results = new ArrayList<>();

        try {
            if (blacklistService.isBlacklisted("ACCOUNT", request.getSenderId())) {
                results.add(RuleResult.builder()
                        .ruleCode("BLACKLISTED_SENDER")
                        .ruleName("Blacklisted Sender")
                        .triggered(true)
                        .weight(1000)
                        .reason("Sender account " + request.getSenderId() + " is blacklisted")
                        .build());
            }

            if (blacklistService.isBlacklisted("ACCOUNT", request.getReceiverId())) {
                results.add(RuleResult.builder()
                        .ruleCode("BLACKLISTED_BENEFICIARY")
                        .ruleName("Blacklisted Beneficiary")
                        .triggered(true)
                        .weight(1000)
                        .reason("Receiver account " + request.getReceiverId() + " is blacklisted")
                        .build());
            }

            if (request.getDeviceId() != null && !request.getDeviceId().isBlank()) {
                if (blacklistService.isBlacklisted("DEVICE", request.getDeviceId())) {
                    results.add(RuleResult.builder()
                            .ruleCode("BLACKLISTED_DEVICE")
                            .ruleName("Blacklisted Device")
                            .triggered(true)
                            .weight(800)
                            .reason("Device " + request.getDeviceId() + " is blacklisted")
                            .build());
                }
            }

            if (request.getSenderIp() != null && !request.getSenderIp().isBlank()) {
                if (blacklistService.isBlacklisted("IP", request.getSenderIp())) {
                    results.add(RuleResult.builder()
                            .ruleCode("SUSPICIOUS_IP")
                            .ruleName("Suspicious IP Address")
                            .triggered(true)
                            .weight(300)
                            .reason("IP " + request.getSenderIp() + " is on the blacklist (VPN/TOR/Proxy)")
                            .build());
                }
            }
        } catch (Exception e) {
            log.error("Blacklist check failed for txn {}: {}", request.getTransactionId(), e.getMessage());
        }

        return results;
    }
}
