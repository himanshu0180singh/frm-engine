package com.company.frm.event;

import com.company.frm.enums.Channel;
import com.company.frm.enums.Decision;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudEvent {

    private String eventId;
    private String transactionId;
    private String customerId;
    private Channel channel;
    private BigDecimal amount;
    private Decision decision;
    private int totalScore;
    private String decisionReason;

    @Builder.Default
    private LocalDateTime eventTimestamp = LocalDateTime.now();
}
