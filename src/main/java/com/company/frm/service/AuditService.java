package com.company.frm.service;

import com.company.frm.dto.FraudCheckResponse;
import com.company.frm.dto.RuleResult;
import com.company.frm.entity.FrmAlert;
import com.company.frm.entity.FrmAuditLog;
import com.company.frm.enums.Decision;
import com.company.frm.repository.FrmAlertRepository;
import com.company.frm.repository.FrmAuditLogRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final FrmAuditLogRepository auditLogRepository;
    private final FrmAlertRepository alertRepository;
    private final ObjectMapper objectMapper;

    @Async
    @Transactional
    public void logDecision(com.company.frm.dto.TransactionRequest request,
                            FraudCheckResponse response) {
        try {
            String triggeredRulesJson = serializeRules(response.getTriggeredRules());

            FrmAuditLog auditLog = FrmAuditLog.builder()
                    .transactionId(request.getTransactionId())
                    .channel(request.getChannel().name())
                    .senderId(request.getSenderId())
                    .receiverId(request.getReceiverId())
                    .amount(request.getAmount())
                    .currency(request.getCurrency())
                    .decision(response.getDecision().name())
                    .riskScore(response.getRiskScore())
                    .triggeredRules(triggeredRulesJson)
                    .deviceId(request.getDeviceId())
                    .senderIp(request.getSenderIp())
                    .latitude(request.getLatitude() != null ? BigDecimal.valueOf(request.getLatitude()) : null)
                    .longitude(request.getLongitude() != null ? BigDecimal.valueOf(request.getLongitude()) : null)
                    .txnTime(request.getTxnTime())
                    .evaluationMs(response.getEvaluationMs())
                    .createdAt(LocalDateTime.now())
                    .build();

            FrmAuditLog saved = auditLogRepository.save(auditLog);

            if (response.getDecision() == Decision.REVIEW || response.getDecision() == Decision.BLOCK) {
                createAlert(saved, response);
            }
        } catch (Exception e) {
            log.error("Failed to persist audit log for txn {}: {}", request.getTransactionId(), e.getMessage());
        }
    }

    private void createAlert(FrmAuditLog auditLog, FraudCheckResponse response) {
        String priority = response.getDecision() == Decision.BLOCK ? "HIGH" : "MEDIUM";
        FrmAlert alert = FrmAlert.builder()
                .transactionId(auditLog.getTransactionId())
                .auditId(auditLog.getAuditId())
                .alertStatus("OPEN")
                .priority(priority)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        alertRepository.save(alert);
        log.info("Alert created for txn {} with priority {}", auditLog.getTransactionId(), priority);
    }

    private String serializeRules(List<RuleResult> rules) {
        if (rules == null || rules.isEmpty()) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(rules);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize triggered rules: {}", e.getMessage());
            return "[]";
        }
    }
}
