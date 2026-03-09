package com.company.frm.repository;

import com.company.frm.entity.FrmRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FrmRuleRepository extends JpaRepository<FrmRule, Integer> {

    List<FrmRule> findByIsActiveTrue();
}
