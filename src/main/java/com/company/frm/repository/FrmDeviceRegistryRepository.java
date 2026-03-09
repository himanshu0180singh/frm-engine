package com.company.frm.repository;

import com.company.frm.entity.FrmDeviceRegistry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FrmDeviceRegistryRepository extends JpaRepository<FrmDeviceRegistry, Long> {

    Optional<FrmDeviceRegistry> findByDeviceIdAndCustomerId(String deviceId, String customerId);

    @Query("SELECT COUNT(DISTINCT d.customerId) FROM FrmDeviceRegistry d WHERE d.deviceId = :deviceId")
    long countDistinctCustomersByDeviceId(String deviceId);
}
