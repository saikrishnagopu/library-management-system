package com.example.library.schedule;

import com.example.library.service.NotificationDispatchService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Cron: reads pending rows from the {@code notifications} table (produced by {@link AsyncEventProcessingScheduler}). */
@Component
public class NotificationDispatchScheduler {

    private static final Logger log = LoggerFactory.getLogger(NotificationDispatchScheduler.class);

    private final NotificationDispatchService notificationDispatchService;

    @Value("${library.notification.dispatch.cron:0/30 * * * * *}")
    private String dispatchCronExpression;

    public NotificationDispatchScheduler(NotificationDispatchService notificationDispatchService) {
        this.notificationDispatchService = notificationDispatchService;
    }

    @PostConstruct
    void logRegisteredSchedule() {
        log.debug(
                "LIBRARY_CRON | notifications dispatch scheduler registered; cron=\"{}\" (enable DEBUG for com.example.library to see)",
                dispatchCronExpression);
    }

    @Scheduled(cron = "${library.notification.dispatch.cron:0/30 * * * * *}")
    public void dispatchPendingNotifications() {
        log.debug("LIBRARY_CRON | notifications dispatch job started");
        notificationDispatchService.processPendingBatch();
        log.debug("LIBRARY_CRON | notifications dispatch job finished");
    }
}
