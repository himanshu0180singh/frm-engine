package com.company.frm.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "frm_audit_log")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FrmAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "audit_id")
    private Long auditId;

    @Column(name = "transaction_id", nullable = false)
    private String transactionId;

    @Column(name = "channel", nullable = false)
    private String channel;

    @Column(name = "sender_id")
    private String senderId;

    @Column(name = "receiver_id")
    private String receiverId;

    @Column(name = "amount")
    private BigDecimal amount;

    @Column(name = "currency", nullable = false)
    private String currency;

    @Column(name = "decision", nullable = false)
    private String decision;

    @Column(name = "risk_score", nullable = false)
    private int riskScore;

    @Column(name = "triggered_rules", columnDefinition = "jsonb")
    private String triggeredRules;

    @Column(name = "device_id")
    private String deviceId;

    @Column(name = "sender_ip")
    private String senderIp;

    @Column(name = "latitude")
    private BigDecimal latitude;

    @Column(name = "longitude")
    private BigDecimal longitude;

    @Column(name = "txn_time")
    private LocalDateTime txnTime;

    @Column(name = "evaluation_ms")
    private Long evaluationMs;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
