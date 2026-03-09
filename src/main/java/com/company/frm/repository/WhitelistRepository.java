package com.company.frm.repository;

import com.company.frm.entity.FrmWhitelist;
import com.company.frm.enums.EntityType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface WhitelistRepository extends JpaRepository<FrmWhitelist, Long> {

    boolean existsByEntityTypeAndEntityValueAndIsActiveTrueAndExpiresAtIsNull(
            EntityType entityType, String entityValue);

    boolean existsByEntityTypeAndEntityValueAndIsActiveTrueAndExpiresAtAfter(
            EntityType entityType, String entityValue, LocalDateTime now);
}
