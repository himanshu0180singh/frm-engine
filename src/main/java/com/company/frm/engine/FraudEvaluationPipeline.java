package com.company.frm.engine;

import com.company.frm.dto.FraudCheckResponse;
import com.company.frm.dto.RuleResult;
import com.company.frm.dto.TransactionRequest;
import com.company.frm.engine.checker.BlacklistChecker;
import com.company.frm.engine.checker.SanityChecker;
import com.company.frm.engine.checker.WhitelistChecker;
import com.company.frm.engine.rule.FraudRule;
import com.company.frm.engine.scoring.RiskScoreAggregator;
import com.company.frm.enums.Decision;
import com.company.frm.enums.SystemMode;
import com.company.frm.event.FraudEventPublisher;
import com.company.frm.service.AuditService;
import com.company.frm.service.CustomerProfileService;
import com.company.frm.service.RuleConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FraudEvaluationPipeline {

    private final SanityChecker sanityChecker;
    private final BlacklistChecker blacklistChecker;
    private final WhitelistChecker whitelistChecker;
    private final List<FraudRule> fraudRules;
    private final RiskScoreAggregator scoreAggregator;
    private final RuleConfigService ruleConfigService;
    private final AuditService auditService;
    private final CustomerProfileService customerProfileService;
    private final FraudEventPublisher fraudEventPublisher;

    @Value("${frm.system-mode:SHADOW}")
    private String systemMode;

    public FraudCheckResponse evaluate(TransactionRequest request) {
        long startMs = System.currentTimeMillis();
        boolean isShadow = SystemMode.SHADOW.name().equalsIgnoreCase(systemMode);

        // ── Step 1: Sanity check ──────────────────────────────────────────────
        String sanityError = sanityChecker.check(request);
        if (sanityError != null) {
            log.warn("Sanity check failed for txn {}: {}", request.getTransactionId(), sanityError);
            FraudCheckResponse response = buildResponse(request, Decision.BLOCK, 1000,
                    List.of(RuleResult.builder()
                            .ruleCode("SANITY_FAIL")
                            .ruleName("Sanity Check Failure")
                            .triggered(true)
                            .weight(1000)
                            .reason(sanityError)
                            .build()),
                    startMs, isShadow);
            asyncPostProcess(request, response);
            return response;
        }

        // ── Step 2: Whitelist fast-exit ───────────────────────────────────────
        if (whitelistChecker.isSenderWhitelisted(request)) {
            log.debug("Sender {} is whitelisted — fast exit ALLOW", request.getSenderId());
            FraudCheckResponse response = buildResponse(request, Decision.ALLOW, 0,
                    List.of(), startMs, isShadow);
            asyncPostProcess(request, response);
            return response;
        }

        // ── Step 3: Blacklist pre-check ───────────────────────────────────────
        List<RuleResult> allResults = new ArrayList<>(blacklistChecker.check(request));

        // Fast-exit if blacklisted (weight >= block threshold)
        int blacklistScore = allResults.stream().mapToInt(RuleResult::getWeight).sum();
        if (blacklistScore >= ruleConfigService.getBlockThreshold()) {
            Decision decision = isShadow ? Decision.ALLOW : Decision.BLOCK;
            log.info("Blacklist match for txn {} — decision: {}", request.getTransactionId(), decision);
            FraudCheckResponse response = buildResponse(request, decision, blacklistScore,
                    allResults, startMs, isShadow);
            asyncPostProcess(request, response);
            return response;
        }

        // ── Step 4: Rule execution ────────────────────────────────────────────
        for (FraudRule rule : fraudRules) {
            try {
                RuleResult result = rule.evaluate(request);
                if (result.isTriggered()) {
                    allResults.add(result);
                    log.debug("Rule {} triggered for txn {} — weight {}", result.getRuleCode(),
                            request.getTransactionId(), result.getWeight());
                }
            } catch (Exception e) {
                log.error("Rule {} threw exception for txn {}: {}", rule.getRuleCode(),
                        request.getTransactionId(), e.getMessage());
            }
        }

        // ── Step 5: Scoring & decision ────────────────────────────────────────
        int totalScore  = scoreAggregator.aggregate(allResults);
        Decision rawDecision = scoreAggregator.decide(totalScore);

        // In SHADOW mode always return ALLOW but log the real decision
        Decision effectiveDecision = isShadow ? Decision.ALLOW : rawDecision;
        if (isShadow && rawDecision != Decision.ALLOW) {
            log.info("[SHADOW] txn {} would have been {} (score={})", request.getTransactionId(), rawDecision, totalScore);
        }

        FraudCheckResponse response = buildResponse(request, effectiveDecision, totalScore,
                allResults, startMs, isShadow);

        // ── Step 6: Async post-processing ─────────────────────────────────────
        asyncPostProcess(request, response);

        return response;
    }

    private void asyncPostProcess(TransactionRequest request, FraudCheckResponse response) {
        auditService.logDecision(request, response);
        customerProfileService.updateProfileAfterTxn(request);
        fraudEventPublisher.publish(request, response);
    }

    private FraudCheckResponse buildResponse(TransactionRequest request,
                                              Decision decision,
                                              int score,
                                              List<RuleResult> results,
                                              long startMs,
                                              boolean shadow) {
        long evalMs = System.currentTimeMillis() - startMs;
        return FraudCheckResponse.builder()
                .transactionId(request.getTransactionId())
                .decision(decision)
                .riskScore(score)
                .triggeredRules(results.stream().filter(RuleResult::isTriggered).toList())
                .message(buildMessage(decision, shadow))
                .evaluationMs(evalMs)
                .evaluatedAt(LocalDateTime.now())
                .shadowMode(shadow)
                .build();
    }

    private String buildMessage(Decision decision, boolean shadow) {
        if (shadow) return "SHADOW MODE — engine is in observation-only mode";
        return switch (decision) {
            case ALLOW  -> "Transaction approved";
            case REVIEW -> "Transaction flagged for manual review";
            case BLOCK  -> "Transaction blocked due to fraud risk";
        };
    }
}
