package com.company.frm.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "frm_rule_parameter")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FrmRuleParameter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "param_id")
    private Integer paramId;

    @Column(name = "rule_id", nullable = false)
    private Integer ruleId;

    @Column(name = "param_key", nullable = false)
    private String paramKey;

    @Column(name = "param_value", nullable = false)
    private String paramValue;
}
