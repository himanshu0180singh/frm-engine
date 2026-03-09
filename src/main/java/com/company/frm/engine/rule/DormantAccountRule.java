package com.company.frm.engine.rule;

import com.company.frm.dto.RuleResult;
import com.company.frm.dto.TransactionRequest;
import com.company.frm.entity.FrmCustomerProfile;
import com.company.frm.service.CustomerProfileService;
import com.company.frm.service.RuleConfigService;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Component
public class DormantAccountRule extends AbstractFraudRule {

    private final CustomerProfileService customerProfileService;

    public DormantAccountRule(RuleConfigService ruleConfigService,
                               CustomerProfileService customerProfileService) {
        super(ruleConfigService);
        this.customerProfileService = customerProfileService;
    }

    @Override
    public String getRuleCode() {
        return "DORMANT_ACCOUNT";
    }

    @Override
    public RuleResult evaluate(TransactionRequest request) {
        if (!appliesToChannel(request)) return notTriggered();
        int dormantDays = getIntParam("dormant_days", 180);

        Optional<FrmCustomerProfile> profileOpt = customerProfileService.getProfile(request.getSenderId());
        if (profileOpt.isEmpty()) {
            return notTriggered();
        }
        FrmCustomerProfile profile = profileOpt.get();
        if (profile.getLastTxnDate() == null) {
            return notTriggered();
        }
        long daysSinceLast = ChronoUnit.DAYS.between(profile.getLastTxnDate(), LocalDateTime.now());
        if (daysSinceLast > dormantDays) {
            return triggered("Account inactive for " + daysSinceLast + " days (threshold: " + dormantDays + ")");
        }
        return notTriggered();
    }
}
