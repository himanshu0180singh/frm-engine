package com.company.frm.engine.rules;

import com.company.frm.dto.RuleResult;
import com.company.frm.dto.TransactionRequest;
import com.company.frm.entity.FrmCustomerProfile;
import com.company.frm.entity.FrmRule;
import com.company.frm.service.CustomerProfileService;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

public class AmountDeviationRule extends AbstractFraudRule {

    private final CustomerProfileService customerProfileService;

    public AmountDeviationRule(FrmRule ruleConfig, Map<String, String> params,
                               CustomerProfileService customerProfileService) {
        super(ruleConfig, params);
        this.customerProfileService = customerProfileService;
    }

    @Override
    public boolean isApplicable(TransactionRequest request) {
        return request.getCustomerId() != null && request.getAmount() != null;
    }

    @Override
    public RuleResult evaluate(TransactionRequest request) {
        double deviationMultiplier = getDoubleParam("DEVIATION_MULTIPLIER", 5.0);
        int minTxnCount = getIntParam("MIN_TXN_COUNT", 5);

        Optional<FrmCustomerProfile> profileOpt =
                customerProfileService.findByCustomerId(request.getCustomerId());
        if (profileOpt.isPresent()) {
            FrmCustomerProfile profile = profileOpt.get();
            if (profile.getTotalTxnCount() >= minTxnCount
                    && profile.getAvgTxnAmount() != null
                    && profile.getAvgTxnAmount().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal threshold = profile.getAvgTxnAmount()
                        .multiply(BigDecimal.valueOf(deviationMultiplier));
                if (request.getAmount().compareTo(threshold) > 0) {
                    return fired(String.format(
                            "Amount %.2f is %.1fx above customer average %.2f",
                            request.getAmount(), deviationMultiplier,
                            profile.getAvgTxnAmount()));
                }
            }
        }
        return notFired();
    }
}
