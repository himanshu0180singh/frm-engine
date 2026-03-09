package com.company.frm.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "frm_global_config")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FrmGlobalConfig {

    @Id
    @Column(name = "config_key")
    private String configKey;

    @Column(name = "config_value", nullable = false)
    private String configValue;

    @Column(name = "description")
    private String description;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
