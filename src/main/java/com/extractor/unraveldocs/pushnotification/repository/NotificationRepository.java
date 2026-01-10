package com.extractor.unraveldocs.pushnotification.repository;

import com.extractor.unraveldocs.pushnotification.datamodel.NotificationType;
import com.extractor.unraveldocs.pushnotification.model.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Repository for Notification entity operations.
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, String> {

    /**
     * Find all notifications for a user, ordered by creation date descending.
     */
    Page<Notification> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    /**
     * Find all notifications for a user by type.
     */
    Page<Notification> findByUserIdAndTypeOrderByCreatedAtDesc(
            String userId, NotificationType type, Pageable pageable);

    /**
     * Find unread notifications for a user.
     */
    Page<Notification> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(String userId, Pageable pageable);

    /**
     * Count unread notifications for a user.
     */
    long countByUserIdAndIsReadFalse(String userId);

    /**
     * Mark all notifications as read for a user.
     */
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = :readAt WHERE n.user.id = :userId AND n.isRead = false")
    int markAllAsRead(@Param("userId") String userId, @Param("readAt") OffsetDateTime readAt);

    /**
     * Delete old notifications (for cleanup job).
     */
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.createdAt < :cutoffDate")
    int deleteOlderThan(@Param("cutoffDate") OffsetDateTime cutoffDate);

    /**
     * Find notifications by user and type for deduplication.
     */
    List<Notification> findByUserIdAndTypeAndCreatedAtAfter(
            String userId, NotificationType type, OffsetDateTime after);
}
