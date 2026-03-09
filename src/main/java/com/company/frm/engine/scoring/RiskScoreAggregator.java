package com.company.frm.engine.scoring;

import com.company.frm.dto.RuleResult;
import com.company.frm.enums.Decision;
import com.company.frm.service.RuleConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class RiskScoreAggregator {

    private final RuleConfigService ruleConfigService;

    /**
     * Sums up risk weights from all triggered rules.
     * Scale: 0–1000+ raw sum (capped at 1000 for display purposes).
     */
    public int aggregate(List<RuleResult> triggeredRules) {
        return triggeredRules.stream()
                .filter(RuleResult::isTriggered)
                .mapToInt(RuleResult::getWeight)
                .sum();
    }

    /**
     * Maps a raw score to a decision based on DB-configured thresholds.
     * ALLOW  = 0 to reviewThreshold - 1
     * REVIEW = reviewThreshold to blockThreshold - 1
     * BLOCK  = blockThreshold and above
     */
    public Decision decide(int score) {
        int blockThreshold  = ruleConfigService.getBlockThreshold();
        int reviewThreshold = ruleConfigService.getReviewThreshold();

        if (score >= blockThreshold) {
            return Decision.BLOCK;
        } else if (score >= reviewThreshold) {
            return Decision.REVIEW;
        }
        return Decision.ALLOW;
    }
}
