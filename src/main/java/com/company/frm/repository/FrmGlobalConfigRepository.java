package com.company.frm.repository;

import com.company.frm.entity.FrmGlobalConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FrmGlobalConfigRepository extends JpaRepository<FrmGlobalConfig, String> {
}
