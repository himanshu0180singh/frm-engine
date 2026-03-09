package com.company.frm.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "frm_device_registry")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FrmDeviceRegistry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "device_id", nullable = false)
    private String deviceId;

    @Column(name = "customer_id", nullable = false)
    private String customerId;

    @Column(name = "device_name")
    private String deviceName;

    @Column(name = "first_seen", nullable = false)
    private LocalDateTime firstSeen;

    @Column(name = "last_seen", nullable = false)
    private LocalDateTime lastSeen;

    @Column(name = "is_trusted", nullable = false)
    private boolean isTrusted;
}
