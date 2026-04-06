package com.example.library.service;

import com.example.library.domain.Notification;
import com.example.library.domain.NotificationStatus;
import com.example.library.repository.NotificationRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Second stage: processes {@code notifications} only (after async events were expanded per wishlist user). */
@Service
public class NotificationDispatchService {

    private static final Logger log = LoggerFactory.getLogger(NotificationDispatchService.class);

    private final NotificationRepository notificationRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Value("${library.notification.dispatch.batch-size:100}")
    private int batchSize;

    public NotificationDispatchService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    /**
     * Locks a batch of pending rows with SKIP LOCKED so clustered nodes do not process the same work.
     * Sends (logs) each notification and marks it PROCESSED.
     */
    @Transactional
    public void processPendingBatch() {
        @SuppressWarnings("unchecked")
        List<Object> rawIds =
                entityManager
                        .createNativeQuery(
                                """
                                SELECT id FROM notifications
                                WHERE status = 'PENDING'
                                ORDER BY id ASC
                                LIMIT ? FOR UPDATE SKIP LOCKED
                                """)
                        .setParameter(1, batchSize)
                        .getResultList();

        List<Long> ids = new ArrayList<>(rawIds.size());
        for (Object o : rawIds) {
            if (o instanceof Number num) {
                ids.add(num.longValue());
            }
        }
        if (ids.isEmpty()) {
            log.debug("notifications dispatch: cron tick, no pending rows to claim (SKIP LOCKED batch empty)");
            return;
        }

        log.debug("notifications dispatch: claimed {} pending notification id(s): {}", ids.size(), ids);

        List<Notification> notifications = notificationRepository.findAllById(ids);
        Instant now = Instant.now();
        for (Notification n : notifications) {
            if (n.getStatus() != NotificationStatus.PENDING) {
                continue;
            }
            log.info(
                    "notifications dispatch: sending id={}, userId={}, bookId={}, type={}",
                    n.getId(),
                    n.getUserId(),
                    n.getBookId(),
                    n.getType());
            log.debug(
                    "Notification prepared for user_id: {}: Book [{}] is now available.",
                    n.getUserId(),
                    n.getBookTitle());
            n.setStatus(NotificationStatus.PROCESSED);
            n.setProcessedAt(now);
            log.debug("notifications dispatch: marked id={} as PROCESSED", n.getId());
        }
    }
}
