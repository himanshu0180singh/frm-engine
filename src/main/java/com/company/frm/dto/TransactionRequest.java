package com.company.frm.dto;

import com.company.frm.enums.Channel;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionRequest {

    @NotBlank(message = "transactionId is required")
    private String transactionId;

    @NotNull(message = "channel is required")
    private Channel channel;

    @NotBlank(message = "senderId is required")
    private String senderId;

    @NotBlank(message = "receiverId is required")
    private String receiverId;

    @NotNull(message = "amount is required")
    @Positive(message = "amount must be positive")
    private BigDecimal amount;

    @NotBlank(message = "currency is required")
    private String currency;

    private String deviceId;
    private String senderIp;
    private Double latitude;
    private Double longitude;
    private String senderCity;

    @NotNull(message = "txnTime is required")
    private LocalDateTime txnTime;

    private String upiVpa;
    private String atmId;
    private boolean internationalTxn;
    private boolean collectRequest;
}
