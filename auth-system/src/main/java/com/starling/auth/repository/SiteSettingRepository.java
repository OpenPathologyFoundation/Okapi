package com.starling.auth.repository;

import com.starling.auth.model.db.SiteSettingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SiteSettingRepository extends JpaRepository<SiteSettingEntity, String> {
}
