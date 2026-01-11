package com.okapi.auth.repository;

import com.okapi.auth.model.db.IdentityEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface IdentityRepository extends JpaRepository<IdentityEntity, Long> {
    Optional<IdentityEntity> findByEmail(String email);

    Optional<IdentityEntity> findByExternalSubject(String externalSubject);

    Optional<IdentityEntity> findByProviderIdAndExternalSubject(String providerId, String externalSubject);
}
