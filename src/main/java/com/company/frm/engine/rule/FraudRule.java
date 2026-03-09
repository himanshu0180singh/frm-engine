package com.company.frm.engine.rule;

import com.company.frm.dto.RuleResult;
import com.company.frm.dto.TransactionRequest;

public interface FraudRule {

    String getRuleCode();

    RuleResult evaluate(TransactionRequest request);
}
