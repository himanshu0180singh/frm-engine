package com.company.frm.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuleResult {

    private String ruleCode;
    private String ruleName;
    private boolean triggered;
    private int weight;
    private String reason;
}
