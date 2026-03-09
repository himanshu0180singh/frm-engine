package com.company.frm.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "frm_alert")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FrmAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "alert_id")
    private Long alertId;

    @Column(name = "transaction_id", nullable = false)
    private String transactionId;

    @Column(name = "audit_id", nullable = false)
    private Long auditId;

    @Column(name = "alert_status", nullable = false)
    private String alertStatus;

    @Column(name = "assigned_to")
    private String assignedTo;

    @Column(name = "priority", nullable = false)
    private String priority;

    @Column(name = "notes")
    private String notes;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
