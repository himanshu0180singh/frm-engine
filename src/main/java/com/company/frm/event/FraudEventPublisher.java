package com.company.frm.event;

import com.company.frm.dto.FraudCheckResponse;
import com.company.frm.dto.TransactionRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class FraudEventPublisher {

    private final KafkaTemplate<String, FraudEvent> kafkaTemplate;

    @Value("${frm.kafka.topic.fraud-events:frm.fraud.events}")
    private String fraudEventsTopic;

    public void publish(TransactionRequest request, FraudCheckResponse response) {
        try {
            FraudEvent event = FraudEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .transactionId(request.getTransactionId())
                    .customerId(request.getCustomerId())
                    .channel(request.getChannel())
                    .amount(request.getAmount())
                    .decision(response.getDecision())
                    .totalScore(response.getTotalScore())
                    .decisionReason(response.getDecisionReason())
                    .build();
            kafkaTemplate.send(fraudEventsTopic, request.getTransactionId(), event);
            log.debug("Published fraud event for txn={} decision={}",
                    request.getTransactionId(), response.getDecision());
        } catch (Exception e) {
            log.error("Failed to publish fraud event for txn={}: {}",
                    request.getTransactionId(), e.getMessage());
        }
    }
}
