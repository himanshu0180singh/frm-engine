package com.company.frm.dto;

import com.company.frm.enums.Decision;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudCheckResponse {

    private String transactionId;
    private Decision decision;
    private int riskScore;
    private List<RuleResult> triggeredRules;
    private String message;
    private long evaluationMs;
    private LocalDateTime evaluatedAt;
    private boolean shadowMode;
}
