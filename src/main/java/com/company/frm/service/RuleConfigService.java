package com.company.frm.service;

import com.company.frm.engine.rules.*;
import com.company.frm.entity.FrmRule;
import com.company.frm.entity.FrmRuleParameter;
import com.company.frm.repository.RuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RuleConfigService {

    private final RuleRepository ruleRepository;
    private final VelocityService velocityService;
    private final CustomerProfileService customerProfileService;

    @Cacheable(value = "rules", key = "#channel")
    public List<FraudRule> getActiveRulesForChannel(String channel) {
        List<FrmRule> dbRules = ruleRepository.findActiveRulesByChannel(channel);
        log.info("Loaded {} active rules for channel {}", dbRules.size(), channel);
        return dbRules.stream()
                .map(this::buildRule)
                .filter(r -> r != null)
                .collect(Collectors.toList());
    }

    private Map<String, String> extractParams(FrmRule rule) {
        if (rule.getParameters() == null) {
            return Map.of();
        }
        return rule.getParameters().stream()
                .collect(Collectors.toMap(
                        FrmRuleParameter::getParamKey,
                        FrmRuleParameter::getParamValue,
                        (a, b) -> a));
    }

    private FraudRule buildRule(FrmRule rule) {
        Map<String, String> params = extractParams(rule);
        return switch (rule.getRuleCode()) {
            case "AMT_HIGH"       -> new HighAmountRule(rule, params);
            case "VEL_COUNT_1H",
                 "VEL_COUNT_24H"  -> new VelocityCountRule(rule, params, velocityService);
            case "VEL_NIGHT"      -> new MidnightTxnRule(rule, params);
            case "BHDOT_DORMANT"  -> new DormantAccountRule(rule, params, customerProfileService);
            case "AMT_DEVIATION"  -> new AmountDeviationRule(rule, params, customerProfileService);
            case "GEO_ANOMALY"    -> new GeoAnomalyRule(rule, params, customerProfileService);
            default -> {
                log.debug("No specific implementation for rule {}, skipping", rule.getRuleCode());
                yield null;
            }
        };
    }
}
