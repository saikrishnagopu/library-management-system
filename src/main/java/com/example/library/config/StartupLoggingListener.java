package com.example.library.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class StartupLoggingListener {

    private static final Logger log = LoggerFactory.getLogger(StartupLoggingListener.class);

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        log.info(
                "=== Library Management is ready. Watch for LIBRARY_CRON lines (async_events ~30s, notifications ~1m) and async_events enqueue on book return ===");
    }
}
