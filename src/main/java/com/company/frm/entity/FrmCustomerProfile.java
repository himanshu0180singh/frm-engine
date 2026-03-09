package com.company.frm.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "frm_customer_profile")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FrmCustomerProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false, unique = true)
    private String customerId;

    @Column(name = "risk_score", nullable = false)
    private int riskScore;

    @Column(name = "risk_tier", nullable = false)
    private String riskTier;

    @Column(name = "total_txn_count", nullable = false)
    private long totalTxnCount;

    @Column(name = "total_txn_amount", nullable = false)
    private BigDecimal totalTxnAmount;

    @Column(name = "avg_txn_amount", nullable = false)
    private BigDecimal avgTxnAmount;

    @Column(name = "last_txn_at")
    private LocalDateTime lastTxnAt;

    @Column(name = "last_txn_channel")
    private String lastTxnChannel;

    @Column(name = "last_known_device_id")
    private String lastKnownDeviceId;

    @Column(name = "last_known_ip")
    private String lastKnownIp;

    @Column(name = "fraud_flag_count", nullable = false)
    private int fraudFlagCount;

    @Column(name = "is_dormant", nullable = false)
    private boolean isDormant;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
