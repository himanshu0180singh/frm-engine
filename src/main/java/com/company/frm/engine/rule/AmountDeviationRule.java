package com.company.frm.engine.rule;

import com.company.frm.dto.RuleResult;
import com.company.frm.dto.TransactionRequest;
import com.company.frm.entity.FrmCustomerProfile;
import com.company.frm.service.CustomerProfileService;
import com.company.frm.service.RuleConfigService;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

@Component
public class AmountDeviationRule extends AbstractFraudRule {

    private final CustomerProfileService customerProfileService;

    public AmountDeviationRule(RuleConfigService ruleConfigService,
                                CustomerProfileService customerProfileService) {
        super(ruleConfigService);
        this.customerProfileService = customerProfileService;
    }

    @Override
    public String getRuleCode() {
        return "AMOUNT_DEVIATION";
    }

    @Override
    public RuleResult evaluate(TransactionRequest request) {
        if (!appliesToChannel(request)) return notTriggered();
        double multiplier = getDoubleParam("deviation_multiplier", 5.0);

        Optional<FrmCustomerProfile> profileOpt = customerProfileService.getProfile(request.getSenderId());
        if (profileOpt.isEmpty()) return notTriggered();

        FrmCustomerProfile profile = profileOpt.get();
        if (profile.getTotalTxnCount() < 5 || profile.getAvgTxnAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return notTriggered();
        }

        BigDecimal threshold = profile.getAvgTxnAmount().multiply(BigDecimal.valueOf(multiplier));
        if (request.getAmount().compareTo(threshold) > 0) {
            return triggered("Amount " + request.getAmount() + " is >" + multiplier + "x customer average of " + profile.getAvgTxnAmount());
        }
        return notTriggered();
    }
}
