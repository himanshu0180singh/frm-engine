package com.company.frm.repository;

import com.company.frm.entity.FrmRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RuleRepository extends JpaRepository<FrmRule, Long> {

    List<FrmRule> findAllByIsActiveTrueOrderByPriorityAsc();

    Optional<FrmRule> findByRuleCode(String ruleCode);

    @Query("SELECT r FROM FrmRule r WHERE r.isActive = true AND r.appliesTo LIKE %:channel%")
    List<FrmRule> findActiveRulesByChannel(String channel);
}
