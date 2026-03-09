package com.company.frm.repository;

import com.company.frm.entity.FrmCustomerProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FrmCustomerProfileRepository extends JpaRepository<FrmCustomerProfile, String> {
}
