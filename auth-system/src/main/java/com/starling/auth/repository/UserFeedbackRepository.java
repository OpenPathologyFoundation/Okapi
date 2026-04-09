package com.starling.auth.repository;

import com.starling.auth.model.db.UserFeedbackEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface UserFeedbackRepository extends JpaRepository<UserFeedbackEntity, UUID> {

    Page<UserFeedbackEntity> findByStatus(String status, Pageable pageable);

    long countByStatus(String status);

    List<UserFeedbackEntity> findByStatusAndArchivedAtBefore(String status, OffsetDateTime cutoff);
}
