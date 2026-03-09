package com.company.frm.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "frm_channel")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FrmChannel {

    @Id
    @Column(name = "channel_code", length = 10)
    private String channelCode;

    @Column(name = "channel_name", nullable = false)
    private String channelName;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
