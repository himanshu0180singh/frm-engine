package com.company.frm.engine.rules;

import com.company.frm.dto.RuleResult;
import com.company.frm.dto.TransactionRequest;
import com.company.frm.entity.FrmCustomerProfile;
import com.company.frm.entity.FrmRule;
import com.company.frm.service.CustomerProfileService;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;

public class DormantAccountRule extends AbstractFraudRule {

    private final CustomerProfileService customerProfileService;

    public DormantAccountRule(FrmRule ruleConfig, Map<String, String> params,
                              CustomerProfileService customerProfileService) {
        super(ruleConfig, params);
        this.customerProfileService = customerProfileService;
    }

    @Override
    public boolean isApplicable(TransactionRequest request) {
        return request.getCustomerId() != null;
    }

    @Override
    public RuleResult evaluate(TransactionRequest request) {
        int dormantDays = getIntParam("DORMANT_DAYS", 90);
        Optional<FrmCustomerProfile> profileOpt =
                customerProfileService.findByCustomerId(request.getCustomerId());
        if (profileOpt.isPresent()) {
            FrmCustomerProfile profile = profileOpt.get();
            if (profile.getLastTxnAt() != null) {
                long daysSinceLastTxn = ChronoUnit.DAYS.between(
                        profile.getLastTxnAt(), LocalDateTime.now());
                if (daysSinceLastTxn >= dormantDays) {
                    return fired(String.format(
                            "Account dormant for %d days (threshold: %d days)",
                            daysSinceLastTxn, dormantDays));
                }
            }
        }
        return notFired();
    }
}
