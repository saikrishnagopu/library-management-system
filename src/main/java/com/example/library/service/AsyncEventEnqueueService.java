package com.example.library.service;

import com.example.library.domain.AsyncEvent;
import com.example.library.domain.AsyncEventStatus;
import com.example.library.domain.AvailabilityStatus;
import com.example.library.repository.AsyncEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Writes {@link AsyncEvent} rows only (no wishlist queries) so the API stays fast and multi-node safe. */
@Service
public class AsyncEventEnqueueService {

    private static final Logger log = LoggerFactory.getLogger(AsyncEventEnqueueService.class);

    private final AsyncEventRepository asyncEventRepository;

    public AsyncEventEnqueueService(AsyncEventRepository asyncEventRepository) {
        this.asyncEventRepository = asyncEventRepository;
    }

    /**
     * Records that a book reached the given availability; wishlist expansion runs later via scheduled
     * {@link AsyncEventProcessorService} (safe for multiple nodes, no heavy work on the request thread).
     */
    @Transactional
    public void enqueueBookAvailabilityChange(Long bookId, AvailabilityStatus availabilityStatus) {
        AsyncEvent event = new AsyncEvent();
        event.setBookId(bookId);
        event.setAvailabilityStatus(availabilityStatus);
        event.setStatus(AsyncEventStatus.PENDING);
        AsyncEvent saved = asyncEventRepository.save(event);
        log.info(
                "async_events enqueue: persisted id={}, bookId={}, availabilityStatus={}, status=PENDING (awaiting cron)",
                saved.getId(),
                bookId,
                availabilityStatus);
    }
}
