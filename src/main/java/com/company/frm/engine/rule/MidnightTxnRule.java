package com.company.frm.engine.rule;

import com.company.frm.dto.RuleResult;
import com.company.frm.dto.TransactionRequest;
import com.company.frm.service.RuleConfigService;
import org.springframework.stereotype.Component;

@Component
public class MidnightTxnRule extends AbstractFraudRule {

    public MidnightTxnRule(RuleConfigService ruleConfigService) {
        super(ruleConfigService);
    }

    @Override
    public String getRuleCode() {
        return "MIDNIGHT_TXN";
    }

    @Override
    public RuleResult evaluate(TransactionRequest request) {
        if (!appliesToChannel(request)) return notTriggered();
        int startHour = getIntParam("start_hour", 0);
        int endHour   = getIntParam("end_hour", 5);
        int hour = request.getTxnTime().getHour();
        if (hour >= startHour && hour < endHour) {
            return triggered("Transaction at " + hour + ":00 is within midnight window (" + startHour + ":00-" + endHour + ":00)");
        }
        return notTriggered();
    }
}
