package com.company.frm.repository;

import com.company.frm.entity.FrmBlacklist;
import com.company.frm.enums.EntityType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface BlacklistRepository extends JpaRepository<FrmBlacklist, Long> {

    Optional<FrmBlacklist> findByEntityTypeAndEntityValueAndIsActiveTrue(
            EntityType entityType, String entityValue);

    boolean existsByEntityTypeAndEntityValueAndIsActiveTrueAndExpiresAtIsNull(
            EntityType entityType, String entityValue);

    boolean existsByEntityTypeAndEntityValueAndIsActiveTrueAndExpiresAtAfter(
            EntityType entityType, String entityValue, LocalDateTime now);
}
