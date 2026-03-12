package com.company.frm.engine.rules;

import com.company.frm.dto.RuleResult;
import com.company.frm.dto.TransactionRequest;
import com.company.frm.entity.FrmRule;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
public abstract class AbstractFraudRule implements FraudRule {

    @Getter
    protected final FrmRule ruleConfig;

    protected final Map<String, String> params;

    @Override
    public String getRuleCode() {
        return ruleConfig.getRuleCode();
    }

    protected Optional<String> getParam(String key) {
        return Optional.ofNullable(params.get(key));
    }

    protected int getIntParam(String key, int defaultValue) {
        return getParam(key).map(v -> {
            try {
                return Integer.parseInt(v);
            } catch (NumberFormatException e) {
                log.warn("Invalid integer param {} for rule {}: {}", key, getRuleCode(), v);
                return defaultValue;
            }
        }).orElse(defaultValue);
    }

    protected double getDoubleParam(String key, double defaultValue) {
        return getParam(key).map(v -> {
            try {
                return Double.parseDouble(v);
            } catch (NumberFormatException e) {
                log.warn("Invalid double param {} for rule {}: {}", key, getRuleCode(), v);
                return defaultValue;
            }
        }).orElse(defaultValue);
    }

    protected RuleResult notFired() {
        return RuleResult.builder()
                .ruleCode(getRuleCode())
                .ruleName(ruleConfig.getRuleName())
                .fired(false)
                .score(0)
                .build();
    }

    protected RuleResult fired(String reason) {
        return RuleResult.builder()
                .ruleCode(getRuleCode())
                .ruleName(ruleConfig.getRuleName())
                .fired(true)
                .score(ruleConfig.getBaseScore())
                .reason(reason)
                .blocking(ruleConfig.isBlocking())
                .build();
    }
}
