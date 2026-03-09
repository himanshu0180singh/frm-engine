package com.company.frm.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "frm_customer_profile")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FrmCustomerProfile {

    @Id
    @Column(name = "customer_id")
    private String customerId;

    @Column(name = "customer_name")
    private String customerName;

    @Column(name = "avg_txn_amount", nullable = false)
    private BigDecimal avgTxnAmount;

    @Column(name = "total_txn_count", nullable = false)
    private long totalTxnCount;

    @Column(name = "last_txn_date")
    private LocalDateTime lastTxnDate;

    @Column(name = "last_txn_city")
    private String lastTxnCity;

    @Column(name = "last_device_id")
    private String lastDeviceId;

    @Column(name = "account_open_date")
    private LocalDate accountOpenDate;

    @Column(name = "risk_tier", nullable = false)
    private String riskTier;

    @Column(name = "known_beneficiaries")
    private String knownBeneficiaries;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
