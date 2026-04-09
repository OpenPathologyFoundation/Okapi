package com.starling.auth.service;

import com.starling.auth.model.db.UserFeedbackEntity;
import com.starling.auth.repository.UserFeedbackRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;

@Component
public class FeedbackPurgeTask {

    private static final Logger log = LoggerFactory.getLogger(FeedbackPurgeTask.class);

    private final UserFeedbackRepository userFeedbackRepository;

    public FeedbackPurgeTask(UserFeedbackRepository userFeedbackRepository) {
        this.userFeedbackRepository = userFeedbackRepository;
    }

    @Scheduled(cron = "0 0 3 * * *")
    public void purgeArchivedFeedback() {
        OffsetDateTime cutoff = OffsetDateTime.now().minusDays(30);
        List<UserFeedbackEntity> stale = userFeedbackRepository
                .findByStatusAndArchivedAtBefore("archived", cutoff);
        if (!stale.isEmpty()) {
            userFeedbackRepository.deleteAll(stale);
            log.info("Purged {} archived feedback items older than 30 days", stale.size());
        }
    }
}
