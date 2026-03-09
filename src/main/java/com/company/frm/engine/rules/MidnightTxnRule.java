package com.company.frm.engine.rules;

import com.company.frm.dto.RuleResult;
import com.company.frm.dto.TransactionRequest;
import com.company.frm.entity.FrmRule;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;

public class MidnightTxnRule extends AbstractFraudRule {

    public MidnightTxnRule(FrmRule ruleConfig, Map<String, String> params) {
        super(ruleConfig, params);
    }

    @Override
    public boolean isApplicable(TransactionRequest request) {
        return true;
    }

    @Override
    public RuleResult evaluate(TransactionRequest request) {
        int startHour = getIntParam("NIGHT_START_HOUR", 0);
        int endHour = getIntParam("NIGHT_END_HOUR", 4);

        LocalDateTime txnTime = request.getTransactionTimestamp() != null
                ? request.getTransactionTimestamp()
                : LocalDateTime.now();

        int hour = txnTime.getHour();
        if (hour >= startHour && hour < endHour) {
            return fired(String.format(
                    "Transaction at %02d:%02d falls in midnight high-risk window (%02d:00-%02d:00)",
                    hour, txnTime.getMinute(), startHour, endHour));
        }
        return notFired();
    }
}
