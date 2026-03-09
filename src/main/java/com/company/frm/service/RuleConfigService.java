package com.company.frm.service;

import com.company.frm.entity.FrmGlobalConfig;
import com.company.frm.entity.FrmRule;
import com.company.frm.entity.FrmRuleParameter;
import com.company.frm.repository.FrmGlobalConfigRepository;
import com.company.frm.repository.FrmRuleParameterRepository;
import com.company.frm.repository.FrmRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RuleConfigService {

    private final FrmRuleRepository ruleRepository;
    private final FrmRuleParameterRepository parameterRepository;
    private final FrmGlobalConfigRepository configRepository;

    @Cacheable(value = "activeRules")
    public List<FrmRule> getActiveRules() {
        log.debug("Loading active rules from database");
        return ruleRepository.findByIsActiveTrue();
    }

    @Cacheable(value = "ruleParams", key = "#ruleId")
    public Map<String, String> getParamsForRule(Integer ruleId) {
        return parameterRepository.findByRuleId(ruleId).stream()
                .collect(Collectors.toMap(FrmRuleParameter::getParamKey, FrmRuleParameter::getParamValue));
    }

    @Cacheable(value = "globalConfig", key = "#key")
    public String getConfig(String key) {
        return configRepository.findById(key)
                .map(FrmGlobalConfig::getConfigValue)
                .orElse(null);
    }

    public int getReviewThreshold() {
        String val = getConfig("review_threshold");
        return val != null ? Integer.parseInt(val) : 300;
    }

    public int getBlockThreshold() {
        String val = getConfig("block_threshold");
        return val != null ? Integer.parseInt(val) : 600;
    }
}
