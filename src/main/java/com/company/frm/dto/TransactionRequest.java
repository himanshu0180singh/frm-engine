package com.company.frm.dto;

import com.company.frm.enums.Channel;
import com.company.frm.enums.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class TransactionRequest {

    @NotBlank(message = "Transaction ID is required")
    private String transactionId;

    @NotBlank(message = "Customer ID is required")
    private String customerId;

    @NotNull(message = "Channel is required")
    private Channel channel;

    @NotNull(message = "Transaction type is required")
    private TransactionType transactionType;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    @Builder.Default
    private String currency = "INR";

    private String payerAccountNumber;
    private String beneficiaryAccountNumber;
    private String beneficiaryName;
    private String beneficiaryIfsc;
    private String beneficiaryVpa;

    private String deviceId;
    private String ipAddress;
    private String userAgent;

    private Double latitude;
    private Double longitude;

    private LocalDateTime transactionTimestamp;

    private String remarks;
    private String merchantCategory;

    private boolean newPayee;
    private Integer authFailureCount;
}
