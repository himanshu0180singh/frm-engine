package com.company.frm.engine.rule;

import com.company.frm.dto.RuleResult;
import com.company.frm.dto.TransactionRequest;
import com.company.frm.entity.FrmCustomerProfile;
import com.company.frm.service.CustomerProfileService;
import com.company.frm.service.RuleConfigService;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * GEO_ANOMALY — detects impossible travel by comparing current city with last known city.
 * Full implementation would compare GPS coordinates and calculate great-circle distance.
 * Here we use a simplified city-mismatch check as a placeholder for the real geo calculation.
 */
@Component
public class GeoAnomalyRule extends AbstractFraudRule {

    private final CustomerProfileService customerProfileService;

    public GeoAnomalyRule(RuleConfigService ruleConfigService,
                           CustomerProfileService customerProfileService) {
        super(ruleConfigService);
        this.customerProfileService = customerProfileService;
    }

    @Override
    public String getRuleCode() {
        return "GEO_ANOMALY";
    }

    @Override
    public RuleResult evaluate(TransactionRequest request) {
        if (!appliesToChannel(request)) return notTriggered();
        if (request.getSenderCity() == null || request.getSenderCity().isBlank()) {
            return notTriggered();
        }

        Optional<FrmCustomerProfile> profileOpt = customerProfileService.getProfile(request.getSenderId());
        if (profileOpt.isEmpty()) return notTriggered();

        FrmCustomerProfile profile = profileOpt.get();
        if (profile.getLastTxnCity() == null || profile.getLastTxnCity().isBlank()) {
            return notTriggered();
        }

        // Simplified: flag if city changed AND last transaction was very recent
        boolean differentCity = !request.getSenderCity().equalsIgnoreCase(profile.getLastTxnCity());
        boolean recentActivity = profile.getLastTxnDate() != null
                && java.time.temporal.ChronoUnit.MINUTES.between(profile.getLastTxnDate(), request.getTxnTime()) < 60;

        if (differentCity && recentActivity) {
            return triggered("Transaction from " + request.getSenderCity()
                    + " but last transaction was from " + profile.getLastTxnCity()
                    + " within 60 minutes — possible impossible travel");
        }
        return notTriggered();
    }
}
