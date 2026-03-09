package com.company.frm.repository;

import com.company.frm.entity.FrmAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<FrmAuditLog, Long> {

    List<FrmAuditLog> findByCustomerIdOrderByCreatedAtDesc(String customerId);

    List<FrmAuditLog> findByTransactionIdOrderByCreatedAtDesc(String transactionId);
}
