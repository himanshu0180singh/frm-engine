package com.company.frm.repository;

import com.company.frm.entity.FrmAlert;
import com.company.frm.enums.AlertStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AlertRepository extends JpaRepository<FrmAlert, Long> {

    Optional<FrmAlert> findByAlertRef(String alertRef);

    List<FrmAlert> findByCustomerIdOrderByCreatedAtDesc(String customerId);

    List<FrmAlert> findByStatusOrderByCreatedAtDesc(AlertStatus status);
}
