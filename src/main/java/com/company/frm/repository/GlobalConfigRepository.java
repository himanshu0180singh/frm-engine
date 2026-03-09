package com.company.frm.repository;

import com.company.frm.entity.FrmGlobalConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GlobalConfigRepository extends JpaRepository<FrmGlobalConfig, Long> {

    Optional<FrmGlobalConfig> findByConfigKeyAndIsActiveTrue(String configKey);
}
