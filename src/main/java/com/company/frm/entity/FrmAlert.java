package com.company.frm.entity;

import com.company.frm.enums.AlertStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "frm_alert")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FrmAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "alert_ref", nullable = false, unique = true)
    private String alertRef;

    @Column(name = "transaction_id", nullable = false)
    private String transactionId;

    @Column(name = "customer_id", nullable = false)
    private String customerId;

    @Column(name = "channel", nullable = false)
    private String channel;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "total_score", nullable = false)
    private int totalScore;

    @Column(name = "final_decision", nullable = false)
    private String finalDecision;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AlertStatus status;

    @Column(name = "analyst_id")
    private String analystId;

    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    private String resolutionNotes;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

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
