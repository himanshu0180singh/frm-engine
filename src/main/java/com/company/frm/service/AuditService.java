package com.company.frm.service;

import com.company.frm.dto.FraudCheckResponse;
import com.company.frm.dto.TransactionRequest;
import com.company.frm.entity.FrmAuditLog;
import com.company.frm.repository.AuditLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    @Async
    public void saveAuditAsync(TransactionRequest request, FraudCheckResponse response) {
        try {
            FrmAuditLog auditLog = FrmAuditLog.builder()
                    .transactionId(request.getTransactionId())
                    .customerId(request.getCustomerId())
                    .channel(request.getChannel().name())
                    .txnType(request.getTransactionType().name())
                    .amount(request.getAmount())
                    .currency(request.getCurrency() != null ? request.getCurrency() : "INR")
                    .finalDecision(response.getDecision().name())
                    .totalScore(response.getTotalScore())
                    .rulesFired(response.getRuleResults())
                    .processingMs((int) response.getProcessingTimeMs())
                    .build();
            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.error("Failed to save audit log for txn={}: {}",
                    request.getTransactionId(), e.getMessage());
        }
    }
}
