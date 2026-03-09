package com.company.frm.engine.rule;

import com.company.frm.dto.RuleResult;
import com.company.frm.dto.TransactionRequest;
import com.company.frm.service.RuleConfigService;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class HighAmountRule extends AbstractFraudRule {

    public HighAmountRule(RuleConfigService ruleConfigService) {
        super(ruleConfigService);
    }

    @Override
    public String getRuleCode() {
        return "HIGH_AMOUNT";
    }

    @Override
    public RuleResult evaluate(TransactionRequest request) {
        if (!appliesToChannel(request)) return notTriggered();
        long threshold = getLongParam("high_amount_threshold", 200000L);
        if (request.getAmount().compareTo(BigDecimal.valueOf(threshold)) > 0) {
            return triggered("Amount " + request.getAmount() + " exceeds high-amount threshold of " + threshold);
        }
        return notTriggered();
    }
}
