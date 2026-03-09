package com.company.frm.repository;

import com.company.frm.entity.FrmAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FrmAlertRepository extends JpaRepository<FrmAlert, Long> {
}
