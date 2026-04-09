package com.starling.auth.repository;

import com.starling.auth.model.db.EduNamedCollectionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EduNamedCollectionRepository extends JpaRepository<EduNamedCollectionEntity, UUID> {

    List<EduNamedCollectionEntity> findByVisibilityIn(List<String> visibilities);

    List<EduNamedCollectionEntity> findByOwnerId(UUID ownerId);
}
