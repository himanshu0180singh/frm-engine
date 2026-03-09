package com.company.frm.repository;

import com.company.frm.entity.FrmBeneficiaryHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FrmBeneficiaryHistoryRepository extends JpaRepository<FrmBeneficiaryHistory, Long> {

    Optional<FrmBeneficiaryHistory> findBySenderIdAndBeneficiaryId(String senderId, String beneficiaryId);

    long countBySenderId(String senderId);
}
