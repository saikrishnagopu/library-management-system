package com.example.library.schedule;

import com.example.library.service.AsyncEventProcessorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Cron: reads pending {@code async_events}, resolves wishlists, inserts one {@code notifications} row per user who
 * wishlisted that book (when the book is still available). Downstream, {@link NotificationDispatchScheduler} sends
 * those notifications.
 */
@Component
public class AsyncEventProcessingScheduler {

    private static final Logger log = LoggerFactory.getLogger(AsyncEventProcessingScheduler.class);

    private final AsyncEventProcessorService asyncEventProcessorService;

    public AsyncEventProcessingScheduler(AsyncEventProcessorService asyncEventProcessorService) {
        this.asyncEventProcessorService = asyncEventProcessorService;
    }

    @Scheduled(cron = "${library.async-event.process.cron:0/30 * * * * *}")
    public void processAsyncEvents() {
        log.debug("LIBRARY_CRON | async_events job started");
        asyncEventProcessorService.processPendingBatch();
        log.debug("LIBRARY_CRON | async_events job finished");
    }
}
