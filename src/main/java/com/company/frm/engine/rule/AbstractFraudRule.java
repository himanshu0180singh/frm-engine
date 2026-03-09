package com.company.frm.engine.rule;

import com.company.frm.dto.RuleResult;
import com.company.frm.dto.TransactionRequest;
import com.company.frm.entity.FrmRule;
import com.company.frm.service.RuleConfigService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@RequiredArgsConstructor
@Slf4j
public abstract class AbstractFraudRule implements FraudRule {

    protected final RuleConfigService ruleConfigService;

    protected FrmRule ruleEntity;
    protected Map<String, String> params;

    @PostConstruct
    public void init() {
        ruleConfigService.getActiveRules().stream()
                .filter(r -> r.getRuleCode().equals(getRuleCode()))
                .findFirst()
                .ifPresent(r -> {
                    this.ruleEntity = r;
                    this.params = ruleConfigService.getParamsForRule(r.getRuleId());
                    log.debug("Loaded rule config for {}", getRuleCode());
                });
    }

    protected RuleResult notTriggered() {
        String name = ruleEntity != null ? ruleEntity.getRuleName() : getRuleCode();
        return RuleResult.builder()
                .ruleCode(getRuleCode())
                .ruleName(name)
                .triggered(false)
                .weight(0)
                .build();
    }

    protected RuleResult triggered(String reason) {
        int weight = ruleEntity != null ? ruleEntity.getRiskWeight() : 0;
        String name  = ruleEntity != null ? ruleEntity.getRuleName() : getRuleCode();
        return RuleResult.builder()
                .ruleCode(getRuleCode())
                .ruleName(name)
                .triggered(true)
                .weight(weight)
                .reason(reason)
                .build();
    }

    protected int getIntParam(String key, int defaultValue) {
        if (params == null) return defaultValue;
        String val = params.get(key);
        if (val == null) return defaultValue;
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    protected long getLongParam(String key, long defaultValue) {
        if (params == null) return defaultValue;
        String val = params.get(key);
        if (val == null) return defaultValue;
        try {
            return Long.parseLong(val);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    protected double getDoubleParam(String key, double defaultValue) {
        if (params == null) return defaultValue;
        String val = params.get(key);
        if (val == null) return defaultValue;
        try {
            return Double.parseDouble(val);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    protected boolean appliesToChannel(TransactionRequest request) {
        if (ruleEntity == null) return true;
        String channels = ruleEntity.getApplicableChannels();
        if ("ALL".equalsIgnoreCase(channels)) return true;
        return channels.contains(request.getChannel().name());
    }
}
