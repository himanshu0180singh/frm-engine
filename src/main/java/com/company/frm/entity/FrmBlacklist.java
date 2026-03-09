package com.company.frm.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "frm_blacklist")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FrmBlacklist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "blacklist_id")
    private Long blacklistId;

    @Column(name = "entity_type", nullable = false)
    private String entityType;

    @Column(name = "entity_value", nullable = false)
    private String entityValue;

    @Column(name = "reason")
    private String reason;

    @Column(name = "added_by")
    private String addedBy;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
}
