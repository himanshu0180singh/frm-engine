package com.company.frm.engine;

import com.company.frm.dto.FraudCheckResponse;
import com.company.frm.dto.RuleResult;
import com.company.frm.dto.TransactionRequest;
import com.company.frm.engine.rules.FraudRule;
import com.company.frm.engine.rules.RiskScoreAggregator;
import com.company.frm.enums.Decision;
import com.company.frm.event.FraudEventPublisher;
import com.company.frm.service.AuditService;
import com.company.frm.service.RuleConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class FraudEvaluationPipeline {

    private final SanityChecker sanityChecker;
    private final WhitelistChecker whitelistChecker;
    private final RuleConfigService ruleConfigService;
    private final RiskScoreAggregator aggregator;
    private final AuditService auditService;
    private final FraudEventPublisher eventPublisher;

    @Value("${frm.engine.allow-threshold:299}")
    private int allowThreshold;

    public FraudCheckResponse evaluate(TransactionRequest request) {
        long startMs = System.currentTimeMillis();
        log.info("FRM evaluation started for txn={} customer={}",
                request.getTransactionId(), request.getCustomerId());

        if (request.getTransactionTimestamp() == null) {
            request.setTransactionTimestamp(LocalDateTime.now());
        }

        // Step 1: Blacklist fast-path
        FraudCheckResponse blacklistResponse = sanityChecker.checkBlacklist(request);
        if (blacklistResponse != null) {
            long elapsed = System.currentTimeMillis() - startMs;
            blacklistResponse.setProcessingTimeMs(elapsed);
            auditService.saveAuditAsync(request, blacklistResponse);
            eventPublisher.publish(request, blacklistResponse);
            return blacklistResponse;
        }

        // Step 2: Whitelist bypass
        if (whitelistChecker.isWhitelisted(request)) {
            FraudCheckResponse allowedResponse = FraudCheckResponse.builder()
                    .transactionId(request.getTransactionId())
                    .customerId(request.getCustomerId())
                    .decision(Decision.ALLOW)
                    .totalScore(0)
                    .decisionReason("Entity is whitelisted")
                    .ruleResults(List.of())
                    .processingTimeMs(System.currentTimeMillis() - startMs)
                    .build();
            auditService.saveAuditAsync(request, allowedResponse);
            return allowedResponse;
        }

        // Step 3: Rule evaluation
        List<FraudRule> rules = ruleConfigService.getActiveRulesForChannel(
                request.getChannel().name());
        List<RuleResult> results = new ArrayList<>();

        for (FraudRule rule : rules) {
            try {
                if (rule.isApplicable(request)) {
                    RuleResult result = rule.evaluate(request);
                    results.add(result);
                    if (result.isFired()) {
                        log.debug("Rule {} fired for txn={} score={}",
                                result.getRuleCode(), request.getTransactionId(), result.getScore());
                    }
                }
            } catch (Exception e) {
                log.error("Error evaluating rule {} for txn={}: {}",
                        rule.getRuleCode(), request.getTransactionId(), e.getMessage());
            }
        }

        // Step 4: Score aggregation
        FraudCheckResponse response = aggregator.aggregate(
                request.getTransactionId(), request.getCustomerId(), results);

        long elapsed = System.currentTimeMillis() - startMs;
        response.setProcessingTimeMs(elapsed);

        log.info("FRM evaluation complete: txn={} decision={} score={} elapsed={}ms",
                request.getTransactionId(), response.getDecision(),
                response.getTotalScore(), elapsed);

        // Step 5: Persist audit & publish event
        auditService.saveAuditAsync(request, response);
        if (response.getDecision() != Decision.ALLOW) {
            eventPublisher.publish(request, response);
        }

        return response;
    }
}
