package com.company.frm.event;

import com.company.frm.dto.FraudCheckResponse;
import com.company.frm.dto.RuleResult;
import com.company.frm.dto.TransactionRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class FraudEventPublisher {

    private final KafkaTemplate<String, FraudEvent> kafkaTemplate;

    @Value("${frm.kafka.topic.fraud-events:fraud-events}")
    private String fraudEventsTopic;

    @Async
    public void publish(TransactionRequest request, FraudCheckResponse response) {
        try {
            FraudEvent event = FraudEvent.builder()
                    .transactionId(request.getTransactionId())
                    .channel(request.getChannel().name())
                    .senderId(request.getSenderId())
                    .receiverId(request.getReceiverId())
                    .amount(request.getAmount())
                    .currency(request.getCurrency())
                    .decision(response.getDecision())
                    .riskScore(response.getRiskScore())
                    .triggeredRuleCodes(
                            response.getTriggeredRules() == null ? java.util.List.of() :
                            response.getTriggeredRules().stream()
                                    .map(RuleResult::getRuleCode)
                                    .toList())
                    .txnTime(request.getTxnTime())
                    .eventTime(LocalDateTime.now())
                    .shadowMode(response.isShadowMode())
                    .build();

            kafkaTemplate.send(fraudEventsTopic, request.getTransactionId(), event);
            log.debug("Published FraudEvent to {} for txn {}", fraudEventsTopic, request.getTransactionId());
        } catch (Exception e) {
            log.error("Failed to publish FraudEvent for txn {}: {}", request.getTransactionId(), e.getMessage());
        }
    }
}
