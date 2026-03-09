package com.company.frm.engine.rule;

import com.company.frm.dto.RuleResult;
import com.company.frm.dto.TransactionRequest;
import com.company.frm.service.RuleConfigService;
import com.company.frm.service.VelocityService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class VelocityCountRule extends AbstractFraudRule {

    private final VelocityService velocityService;

    public VelocityCountRule(RuleConfigService ruleConfigService, VelocityService velocityService) {
        super(ruleConfigService);
        this.velocityService = velocityService;
    }

    @Override
    public String getRuleCode() {
        return "VELOCITY_COUNT";
    }

    @Override
    public RuleResult evaluate(TransactionRequest request) {
        if (!appliesToChannel(request)) return notTriggered();
        int maxCount = getIntParam("max_count", 5);
        int windowMinutes = getIntParam("window_minutes", 10);
        try {
            long count = velocityService.recordAndGetCount(
                    request.getSenderId(), request.getTransactionId(), windowMinutes);
            if (count > maxCount) {
                return triggered(count + " transactions in " + windowMinutes + " minutes (max " + maxCount + ")");
            }
        } catch (Exception e) {
            log.warn("Redis unavailable for VELOCITY_COUNT rule, defaulting to REVIEW signal: {}", e.getMessage());
            return triggered("Redis unavailable; defaulting to review");
        }
        return notTriggered();
    }
}
