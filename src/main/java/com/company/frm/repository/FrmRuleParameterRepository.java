package com.company.frm.repository;

import com.company.frm.entity.FrmRuleParameter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FrmRuleParameterRepository extends JpaRepository<FrmRuleParameter, Integer> {

    List<FrmRuleParameter> findByRuleId(Integer ruleId);
}
