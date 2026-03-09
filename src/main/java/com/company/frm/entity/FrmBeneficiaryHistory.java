package com.company.frm.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "frm_beneficiary_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FrmBeneficiaryHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "sender_id", nullable = false)
    private String senderId;

    @Column(name = "beneficiary_id", nullable = false)
    private String beneficiaryId;

    @Column(name = "first_txn_date", nullable = false)
    private LocalDateTime firstTxnDate;

    @Column(name = "txn_count", nullable = false)
    private int txnCount;

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount;
}
