package com.company.frm.engine.rules;

import com.company.frm.dto.RuleResult;
import com.company.frm.dto.TransactionRequest;
import com.company.frm.entity.FrmRule;
import com.company.frm.service.VelocityService;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
public class VelocityCountRule extends AbstractFraudRule {

    private final VelocityService velocityService;

    public VelocityCountRule(FrmRule ruleConfig, Map<String, String> params,
                             VelocityService velocityService) {
        super(ruleConfig, params);
        this.velocityService = velocityService;
    }

    @Override
    public boolean isApplicable(TransactionRequest request) {
        return request.getCustomerId() != null;
    }

    @Override
    public RuleResult evaluate(TransactionRequest request) {
        int maxCount = getIntParam("MAX_COUNT", 10);
        int windowSeconds = getIntParam("WINDOW_SECONDS", 3600);
        long count = velocityService.getTransactionCount(
                request.getCustomerId(), windowSeconds);
        if (count >= maxCount) {
            return fired(String.format(
                    "Transaction count %d reached limit %d in %ds window",
                    count, maxCount, windowSeconds));
        }
        return notFired();
    }
}
