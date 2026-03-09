package com.company.frm.repository;

import com.company.frm.entity.FrmAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FrmAuditLogRepository extends JpaRepository<FrmAuditLog, Long> {
}
