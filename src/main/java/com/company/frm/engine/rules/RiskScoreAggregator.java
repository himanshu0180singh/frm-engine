package com.company.frm.engine.rules;

import com.company.frm.dto.FraudCheckResponse;
import com.company.frm.dto.RuleResult;
import com.company.frm.enums.Decision;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RiskScoreAggregator {

    @Value("${frm.engine.allow-threshold:299}")
    private int allowThreshold;

    @Value("${frm.engine.review-threshold:599}")
    private int reviewThreshold;

    public FraudCheckResponse aggregate(String transactionId, String customerId,
                                        List<RuleResult> results) {
        int totalScore = 0;
        boolean hasBlockingRule = false;
        StringBuilder reasons = new StringBuilder();

        for (RuleResult result : results) {
            if (result.isFired()) {
                totalScore += result.getScore();
                if (result.isBlocking()) {
                    hasBlockingRule = true;
                }
                if (result.getReason() != null) {
                    if (!reasons.isEmpty()) {
                        reasons.append("; ");
                    }
                    reasons.append(result.getRuleCode()).append(": ").append(result.getReason());
                }
            }
        }

        Decision decision;
        if (hasBlockingRule || totalScore > reviewThreshold) {
            decision = Decision.BLOCK;
        } else if (totalScore > allowThreshold) {
            decision = Decision.REVIEW;
        } else {
            decision = Decision.ALLOW;
        }

        return FraudCheckResponse.builder()
                .transactionId(transactionId)
                .customerId(customerId)
                .decision(decision)
                .totalScore(totalScore)
                .decisionReason(reasons.toString())
                .ruleResults(results)
                .build();
    }
}
