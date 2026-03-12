package com.company.frm.drools;

import com.company.frm.dto.RuleResult;
import com.company.frm.dto.TransactionRequest;
import com.company.frm.entity.FrmCustomerProfile;
import com.company.frm.service.CustomerProfileService;
import com.company.frm.service.VelocityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Evaluates all fraud rules for a single transaction using Drools.
 *
 * <p>Responsibilities:
 * <ol>
 *   <li>Pre-fetch velocity counts from Redis and customer profile from PostgreSQL.</li>
 *   <li>Build a {@link FraudEvaluationContext} POJO that contains every field the
 *       DRL rules need — keeping the rule engine free of Spring bean calls.</li>
 *   <li>Create a stateful {@link KieSession}, insert the context as a fact, fire
 *       all matching rules, collect the resulting {@link RuleResult} objects via
 *       the {@code ruleResults} global, and dispose the session.</li>
 * </ol>
 *
 * <p>This service is called by {@link com.company.frm.engine.FraudEvaluationPipeline}
 * as step 3 of the evaluation pipeline (after blacklist / whitelist checks).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DroolsFraudEvaluator {

    private static final int WINDOW_1H_SECONDS  = 3600;
    private static final int WINDOW_24H_SECONDS = 86400;

    private final KieContainer        kieContainer;
    private final VelocityService     velocityService;
    private final CustomerProfileService customerProfileService;

    /**
     * Evaluates all active Drools fraud rules against the given transaction.
     *
     * @param request the inbound transaction request
     * @return list of {@link RuleResult} for every rule that was evaluated;
     *         only rules that fired have {@code fired=true} and a non-zero score
     */
    public List<RuleResult> evaluate(TransactionRequest request) {

        // ── 1. Pre-fetch context data ────────────────────────────────────────
        long count1h  = velocityService.getTransactionCount(
                request.getCustomerId(), WINDOW_1H_SECONDS);
        long count24h = velocityService.getTransactionCount(
                request.getCustomerId(), WINDOW_24H_SECONDS);

        Optional<FrmCustomerProfile> profileOpt =
                customerProfileService.findByCustomerId(request.getCustomerId());

        // ── 2. Build the Drools fact ─────────────────────────────────────────
        FraudEvaluationContext ctx = FraudEvaluationContext.builder()
                .transactionId(request.getTransactionId())
                .customerId(request.getCustomerId())
                .channel(request.getChannel().name())
                .amount(request.getAmount())
                .transactionTimestamp(request.getTransactionTimestamp())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .ipAddress(request.getIpAddress())
                .deviceId(request.getDeviceId())
                .newPayee(request.isNewPayee())
                .authFailureCount(request.getAuthFailureCount())
                .txnCount1h(count1h)
                .txnCount24h(count24h)
                .avgTxnAmount(profileOpt.map(FrmCustomerProfile::getAvgTxnAmount).orElse(null))
                .totalTxnCount(profileOpt.map(FrmCustomerProfile::getTotalTxnCount).orElse(0L))
                .lastTxnAt(profileOpt.map(FrmCustomerProfile::getLastTxnAt).orElse(null))
                .build();

        // ── 3. Run Drools session ────────────────────────────────────────────
        List<RuleResult> results = new ArrayList<>();
        KieSession session = kieContainer.newKieSession();
        try {
            session.setGlobal("ruleResults", results);
            session.insert(ctx);
            int fired = session.fireAllRules();
            log.debug("Drools fired {} rule(s) for txn={}", fired, request.getTransactionId());
        } catch (Exception e) {
            log.error("Drools session error for txn={}: {}", request.getTransactionId(), e.getMessage(), e);
        } finally {
            session.dispose();
        }

        return results;
    }
}
