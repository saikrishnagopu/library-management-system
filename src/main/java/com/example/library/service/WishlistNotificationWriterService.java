package com.example.library.service;

import com.example.library.domain.Notification;
import com.example.library.domain.NotificationStatus;
import com.example.library.domain.NotificationType;
import com.example.library.repository.NotificationRepository;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WishlistNotificationWriterService {

    private static final Logger log = LoggerFactory.getLogger(WishlistNotificationWriterService.class);

    private final NotificationRepository notificationRepository;

    public WishlistNotificationWriterService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    /** One short transaction per page so parallel workers do not share a persistence context. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void savePage(Long bookId, String bookTitle, List<Long> userIds) {
        if (userIds.isEmpty()) {
            return;
        }
        List<Notification> rows = new ArrayList<>(userIds.size());
        for (Long userId : userIds) {
            Notification n = new Notification();
            n.setType(NotificationType.WISHLIST);
            n.setUserId(userId);
            n.setBookId(bookId);
            n.setBookTitle(bookTitle);
            n.setStatus(NotificationStatus.PENDING);
            rows.add(n);
        }
        notificationRepository.saveAll(rows);
        log.debug(
                "notifications fan-out: inserted {} PENDING row(s) for bookId={}, userIds={}",
                rows.size(),
                bookId,
                userIds);
    }
}
