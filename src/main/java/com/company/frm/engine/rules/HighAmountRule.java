package com.company.frm.engine.rules;

import com.company.frm.dto.RuleResult;
import com.company.frm.dto.TransactionRequest;
import com.company.frm.entity.FrmRule;

import java.math.BigDecimal;
import java.util.Map;

public class HighAmountRule extends AbstractFraudRule {

    public HighAmountRule(FrmRule ruleConfig, Map<String, String> params) {
        super(ruleConfig, params);
    }

    @Override
    public boolean isApplicable(TransactionRequest request) {
        return request.getAmount() != null;
    }

    @Override
    public RuleResult evaluate(TransactionRequest request) {
        double threshold = getDoubleParam("THRESHOLD_AMOUNT", 500000.0);
        if (request.getAmount().compareTo(BigDecimal.valueOf(threshold)) > 0) {
            return fired(String.format("Amount %.2f exceeds high-amount threshold %.2f",
                    request.getAmount(), threshold));
        }
        return notFired();
    }
}
