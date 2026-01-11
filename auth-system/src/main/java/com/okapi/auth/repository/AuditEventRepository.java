package com.okapi.auth.repository;

import com.okapi.auth.model.db.AuditEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditEventRepository extends JpaRepository<AuditEventEntity, Long> {
}
