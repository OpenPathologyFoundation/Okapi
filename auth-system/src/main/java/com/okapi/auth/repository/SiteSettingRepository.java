package com.okapi.auth.repository;

import com.okapi.auth.model.db.SiteSettingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SiteSettingRepository extends JpaRepository<SiteSettingEntity, String> {
}
