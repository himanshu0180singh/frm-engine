package com.company.frm;

import com.company.frm.dto.RuleResult;
import com.company.frm.dto.TransactionRequest;
import com.company.frm.engine.checker.SanityChecker;
import com.company.frm.engine.rule.HighAmountRule;
import com.company.frm.engine.rule.MidnightTxnRule;
import com.company.frm.engine.scoring.RiskScoreAggregator;
import com.company.frm.enums.Channel;
import com.company.frm.enums.Decision;
import com.company.frm.service.RuleConfigService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FrmEngineTests {

    @Mock
    private RuleConfigService ruleConfigService;

    private TransactionRequest buildRequest(BigDecimal amount, LocalDateTime txnTime) {
        return TransactionRequest.builder()
                .transactionId("TXN001")
                .channel(Channel.UPI)
                .senderId("SENDER01")
                .receiverId("RECEIVER01")
                .amount(amount)
                .currency("INR")
                .txnTime(txnTime)
                .build();
    }

    // ── SanityChecker tests ──────────────────────────────────────────────────

    @Test
    void sanityChecker_shouldPassValidRequest() {
        SanityChecker checker = new SanityChecker();
        TransactionRequest req = buildRequest(BigDecimal.valueOf(1000), LocalDateTime.now());
        assertNull(checker.check(req));
    }

    @Test
    void sanityChecker_shouldFailOnSelfTransfer() {
        SanityChecker checker = new SanityChecker();
        TransactionRequest req = TransactionRequest.builder()
                .transactionId("TXN002")
                .channel(Channel.UPI)
                .senderId("SAME")
                .receiverId("SAME")
                .amount(BigDecimal.valueOf(500))
                .currency("INR")
                .txnTime(LocalDateTime.now())
                .build();
        assertNotNull(checker.check(req));
        assertTrue(checker.check(req).contains("self-transfer"));
    }

    @Test
    void sanityChecker_shouldFailOnNegativeAmount() {
        SanityChecker checker = new SanityChecker();
        TransactionRequest req = buildRequest(BigDecimal.valueOf(-100), LocalDateTime.now());
        assertNotNull(checker.check(req));
    }

    // ── HighAmountRule tests ─────────────────────────────────────────────────
    // When no rule entity is loaded (getActiveRules returns empty list),
    // AbstractFraudRule falls back to default parameter values.

    @Test
    void highAmountRule_shouldTriggerAboveThreshold() {
        when(ruleConfigService.getActiveRules()).thenReturn(List.of());

        HighAmountRule rule = new HighAmountRule(ruleConfigService);
        rule.init();

        // Default high_amount_threshold = 200000; amount 250000 should trigger
        TransactionRequest req = buildRequest(BigDecimal.valueOf(250000), LocalDateTime.now());
        RuleResult result = rule.evaluate(req);
        assertTrue(result.isTriggered());
    }

    @Test
    void highAmountRule_shouldNotTriggerBelowThreshold() {
        when(ruleConfigService.getActiveRules()).thenReturn(List.of());

        HighAmountRule rule = new HighAmountRule(ruleConfigService);
        rule.init();

        // Default high_amount_threshold = 200000; amount 50000 should NOT trigger
        TransactionRequest req = buildRequest(BigDecimal.valueOf(50000), LocalDateTime.now());
        RuleResult result = rule.evaluate(req);
        assertFalse(result.isTriggered());
    }

    // ── MidnightTxnRule tests ────────────────────────────────────────────────

    @Test
    void midnightTxnRule_shouldTriggerAt2AM() {
        when(ruleConfigService.getActiveRules()).thenReturn(List.of());

        MidnightTxnRule rule = new MidnightTxnRule(ruleConfigService);
        rule.init();

        // Default start_hour=0, end_hour=5; 02:30 should trigger
        LocalDateTime twoAM = LocalDateTime.now().withHour(2).withMinute(30);
        TransactionRequest req = buildRequest(BigDecimal.valueOf(1000), twoAM);
        RuleResult result = rule.evaluate(req);
        assertTrue(result.isTriggered());
    }

    @Test
    void midnightTxnRule_shouldNotTriggerAt10AM() {
        when(ruleConfigService.getActiveRules()).thenReturn(List.of());

        MidnightTxnRule rule = new MidnightTxnRule(ruleConfigService);
        rule.init();

        // Default start_hour=0, end_hour=5; 10:00 should NOT trigger
        LocalDateTime tenAM = LocalDateTime.now().withHour(10).withMinute(0);
        TransactionRequest req = buildRequest(BigDecimal.valueOf(1000), tenAM);
        RuleResult result = rule.evaluate(req);
        assertFalse(result.isTriggered());
    }

    // ── RiskScoreAggregator tests ────────────────────────────────────────────

    @Test
    void scoreAggregator_shouldReturnAllow() {
        when(ruleConfigService.getBlockThreshold()).thenReturn(600);
        when(ruleConfigService.getReviewThreshold()).thenReturn(300);

        RiskScoreAggregator aggregator = new RiskScoreAggregator(ruleConfigService);
        assertEquals(Decision.ALLOW, aggregator.decide(100));
    }

    @Test
    void scoreAggregator_shouldReturnReview() {
        when(ruleConfigService.getBlockThreshold()).thenReturn(600);
        when(ruleConfigService.getReviewThreshold()).thenReturn(300);

        RiskScoreAggregator aggregator = new RiskScoreAggregator(ruleConfigService);
        assertEquals(Decision.REVIEW, aggregator.decide(400));
    }

    @Test
    void scoreAggregator_shouldReturnBlock() {
        when(ruleConfigService.getBlockThreshold()).thenReturn(600);
        when(ruleConfigService.getReviewThreshold()).thenReturn(300);

        RiskScoreAggregator aggregator = new RiskScoreAggregator(ruleConfigService);
        assertEquals(Decision.BLOCK, aggregator.decide(750));
    }

    @Test
    void scoreAggregator_shouldSumTriggeredWeights() {
        RiskScoreAggregator aggregator = new RiskScoreAggregator(ruleConfigService);
        List<RuleResult> results = List.of(
                RuleResult.builder().ruleCode("R1").triggered(true).weight(250).build(),
                RuleResult.builder().ruleCode("R2").triggered(true).weight(100).build(),
                RuleResult.builder().ruleCode("R3").triggered(false).weight(300).build()
        );
        assertEquals(350, aggregator.aggregate(results));
    }
}
