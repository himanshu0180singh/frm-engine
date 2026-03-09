package com.company.frm.repository;

import com.company.frm.entity.FrmBlacklist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FrmBlacklistRepository extends JpaRepository<FrmBlacklist, Long> {

    Optional<FrmBlacklist> findByEntityTypeAndEntityValueAndIsActiveTrue(String entityType, String entityValue);
}
