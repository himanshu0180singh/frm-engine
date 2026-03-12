package com.company.frm.repository;

import com.company.frm.entity.FrmCustomerProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerProfileRepository extends JpaRepository<FrmCustomerProfile, Long> {

    Optional<FrmCustomerProfile> findByCustomerId(String customerId);
}
