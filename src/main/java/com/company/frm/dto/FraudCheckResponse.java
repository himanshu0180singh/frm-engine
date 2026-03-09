package com.company.frm.dto;

import com.company.frm.enums.Decision;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudCheckResponse {

    private String transactionId;
    private String customerId;
    private Decision decision;
    private int totalScore;
    private String decisionReason;
    private List<RuleResult> ruleResults;
    private boolean shadowMode;

    @Builder.Default
    private LocalDateTime evaluatedAt = LocalDateTime.now();

    private long processingTimeMs;
}
