package com.okapi.auth.repository;

import com.okapi.auth.model.db.WorklistItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorklistRepository extends JpaRepository<WorklistItemEntity, Long>,
        JpaSpecificationExecutor<WorklistItemEntity> {

    Optional<WorklistItemEntity> findByAccessionNumber(String accessionNumber);

    List<WorklistItemEntity> findByAssignedToIdentityId(UUID assignedToIdentityId);

    List<WorklistItemEntity> findByService(String service);

    List<WorklistItemEntity> findByStatus(String status);

    List<WorklistItemEntity> findByPriority(String priority);

    List<WorklistItemEntity> findByAssignedToIdentityIdAndStatusNotIn(UUID assignedToIdentityId, List<String> excludedStatuses);

    long countByAssignedToIdentityId(UUID assignedToIdentityId);

    long countByService(String service);

    long countByStatus(String status);

    long countByPriority(String priority);
}
