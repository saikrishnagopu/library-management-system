package com.example.library.service;

import com.example.library.domain.AsyncEvent;
import com.example.library.domain.AsyncEventStatus;
import com.example.library.domain.AvailabilityStatus;
import com.example.library.domain.Book;
import com.example.library.repository.AsyncEventRepository;
import com.example.library.repository.BookRepository;
import com.example.library.repository.WishlistRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AsyncEventProcessorService {

    private static final Logger log = LoggerFactory.getLogger(AsyncEventProcessorService.class);

    private final AsyncEventRepository asyncEventRepository;
    private final BookRepository bookRepository;
    private final WishlistRepository wishlistRepository;
    private final WishlistNotificationWriterService wishlistNotificationWriterService;
    private final Executor wishlistNotificationExecutor;

    @PersistenceContext
    private EntityManager entityManager;

    @Value("${library.async-event.process.batch-size:50}")
    private int batchSize;

    @Value("${library.wishlist-notification.page-size:100}")
    private int wishlistPageSize;

    public AsyncEventProcessorService(
            AsyncEventRepository asyncEventRepository,
            BookRepository bookRepository,
            WishlistRepository wishlistRepository,
            WishlistNotificationWriterService wishlistNotificationWriterService,
            @Qualifier("wishlistNotificationExecutor") Executor wishlistNotificationExecutor) {
        this.asyncEventRepository = asyncEventRepository;
        this.bookRepository = bookRepository;
        this.wishlistRepository = wishlistRepository;
        this.wishlistNotificationWriterService = wishlistNotificationWriterService;
        this.wishlistNotificationExecutor = wishlistNotificationExecutor;
    }

    /**
     * Claims pending async events with SKIP LOCKED, then creates {@link Notification} rows for wishlisted
     * users when the book is still {@link AvailabilityStatus#AVAILABLE}.
     */
    @Transactional
    public void processPendingBatch() {
        @SuppressWarnings("unchecked")
        List<Object> rawIds =
                entityManager
                        .createNativeQuery(
                                """
                                SELECT id FROM async_events
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
            log.debug("async_events processor: cron tick, no pending rows to claim (SKIP LOCKED batch empty)");
            return;
        }

        log.debug("async_events processor: claimed {} pending row id(s): {}", ids.size(), ids);

        List<AsyncEvent> events = asyncEventRepository.findAllById(ids);
        events.sort(Comparator.comparing(AsyncEvent::getId));
        Instant now = Instant.now();
        for (AsyncEvent event : events) {
            if (event.getStatus() != AsyncEventStatus.PENDING) {
                continue;
            }
            log.debug(
                    "async_events processor: handling event id={}, bookId={}, availabilityStatus={}",
                    event.getId(),
                    event.getBookId(),
                    event.getAvailabilityStatus());
            if (event.getAvailabilityStatus() == AvailabilityStatus.AVAILABLE) {
                bookRepository
                        .findByIdAndDeletedFalse(event.getBookId())
                        .filter(b -> b.getAvailabilityStatus() == AvailabilityStatus.AVAILABLE)
                        .ifPresentOrElse(
                                this::createWishlistNotifications,
                                () ->
                                        log.debug(
                                                "async_events processor: skipping event id={} — book {} missing or not AVAILABLE",
                                                event.getId(),
                                                event.getBookId()));
            }
            event.setStatus(AsyncEventStatus.PROCESSED);
            event.setProcessedAt(now);
            log.debug("async_events processor: marked event id={} as PROCESSED", event.getId());
        }
    }

    /**
     * Loads wishlisted user ids in pages, then inserts {@code notifications} in parallel batches (each batch in its
     * own transaction). If a batch fails after others succeeded, earlier batches stay committed; the async event can
     * be re-driven only if left PENDING (consider idempotency or cleanup for strict exactly-once).
     */
    private void createWishlistNotifications(Book book) {
        Long bookId = book.getId();
        String bookTitle = book.getTitle();
        int pageNumber = 0;
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        while (true) {
            Page<Long> page =
                    wishlistRepository.findUserIdsByBookId(bookId, PageRequest.of(pageNumber, wishlistPageSize));
            if (!page.hasContent()) {
                break;
            }
            List<Long> userIds = new ArrayList<>(page.getContent());
            log.debug(
                    "async_events processor: bookId={} wishlist page {} — {} user id(s): {}",
                    bookId,
                    pageNumber,
                    userIds.size(),
                    userIds);
            futures.add(
                    CompletableFuture.runAsync(
                            () -> wishlistNotificationWriterService.savePage(bookId, bookTitle, userIds),
                            wishlistNotificationExecutor));
            if (!page.hasNext()) {
                break;
            }
            pageNumber++;
        }
        if (futures.isEmpty()) {
            log.debug("async_events processor: bookId={} has no wishlist entries; no notification rows inserted", bookId);
            return;
        }
        try {
            CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
            log.debug(
                    "async_events processor: bookId={} finished fan-out ({} parallel notification batch(es))",
                    bookId,
                    futures.size());
        } catch (CompletionException e) {
            Throwable c = e.getCause();
            if (c instanceof RuntimeException re) {
                throw re;
            }
            throw new IllegalStateException(c);
        }
    }
}
