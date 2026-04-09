package com.starling.auth.repository;

import com.starling.auth.model.db.AuditEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AuditEventRepository extends JpaRepository<AuditEventEntity, UUID>,
        JpaSpecificationExecutor<AuditEventEntity> {
}
