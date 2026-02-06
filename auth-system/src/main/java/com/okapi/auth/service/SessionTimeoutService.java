package com.okapi.auth.service;

import com.okapi.auth.model.Identity;
import com.okapi.auth.model.db.IdentityEntity;
import com.okapi.auth.model.db.SiteSettingEntity;
import com.okapi.auth.repository.IdentityRepository;
import com.okapi.auth.repository.SiteSettingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class SessionTimeoutService {

    private static final Logger log = LoggerFactory.getLogger(SessionTimeoutService.class);
    private static final String KEY = "session.idle.timeout.minutes";
    private static final int DEFAULT_TIMEOUT = 15;
    private static final int MIN_TIMEOUT = 1;
    private static final int MAX_TIMEOUT = 60;

    private final SiteSettingRepository siteSettingRepository;
    private final IdentityRepository identityRepository;

    private volatile int cachedTimeout = DEFAULT_TIMEOUT;
    private volatile long cacheExpiresAt = 0;
    private static final long CACHE_TTL_MS = 60_000;

    public SessionTimeoutService(SiteSettingRepository siteSettingRepository, IdentityRepository identityRepository) {
        this.siteSettingRepository = siteSettingRepository;
        this.identityRepository = identityRepository;
    }

    public int getIdleTimeoutMinutes() {
        long now = System.currentTimeMillis();
        if (now < cacheExpiresAt) {
            return cachedTimeout;
        }
        try {
            int value = siteSettingRepository.findById(KEY)
                    .map(s -> Integer.parseInt(s.getSettingValue()))
                    .orElse(DEFAULT_TIMEOUT);
            cachedTimeout = Math.max(MIN_TIMEOUT, Math.min(MAX_TIMEOUT, value));
        } catch (NumberFormatException e) {
            log.warn("Invalid session timeout value in DB, using default {}", DEFAULT_TIMEOUT);
            cachedTimeout = DEFAULT_TIMEOUT;
        }
        cacheExpiresAt = now + CACHE_TTL_MS;
        return cachedTimeout;
    }

    public void setIdleTimeoutMinutes(Identity actor, int minutes) {
        if (minutes < MIN_TIMEOUT || minutes > MAX_TIMEOUT) {
            throw new IllegalArgumentException(
                    "Timeout must be between " + MIN_TIMEOUT + " and " + MAX_TIMEOUT + " minutes");
        }

        UUID actorId = null;
        if (actor != null) {
            actorId = identityRepository
                    .findByProviderIdAndExternalSubject(actor.getProviderId(), actor.getExternalSubject())
                    .map(IdentityEntity::getIdentityId)
                    .orElse(null);
        }

        SiteSettingEntity setting = siteSettingRepository.findById(KEY)
                .orElse(SiteSettingEntity.builder().settingKey(KEY).build());
        setting.setSettingValue(String.valueOf(minutes));
        setting.setUpdatedAt(OffsetDateTime.now());
        setting.setUpdatedBy(actorId);
        siteSettingRepository.save(setting);

        // Invalidate cache
        cacheExpiresAt = 0;
        cachedTimeout = minutes;

        log.info("Session idle timeout updated to {} minutes by {}", minutes, actorId);
    }
}
