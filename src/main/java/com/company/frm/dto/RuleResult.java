package com.company.frm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuleResult {

    private String ruleCode;
    private String ruleName;
    private boolean fired;
    private int score;
    private String reason;
    private boolean blocking;
}
