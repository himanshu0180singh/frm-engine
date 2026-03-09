package com.company.frm.event;

import com.company.frm.enums.Decision;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudEvent {

    private String transactionId;
    private String channel;
    private String senderId;
    private String receiverId;
    private BigDecimal amount;
    private String currency;
    private Decision decision;
    private int riskScore;
    private List<String> triggeredRuleCodes;
    private LocalDateTime txnTime;
    private LocalDateTime eventTime;
    private boolean shadowMode;
}
