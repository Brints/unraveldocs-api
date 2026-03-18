package com.extractor.unraveldocs.pushnotification.repository;

import com.extractor.unraveldocs.pushnotification.model.NotificationPreferences;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for NotificationPreferences entity operations.
 */
@Repository
public interface NotificationPreferencesRepository extends JpaRepository<NotificationPreferences, String> {

    /**
     * Find notification preferences by user ID.
     */
    Optional<NotificationPreferences> findByUserId(String userId);

    /**
     * Check if preferences exist for a user.
     */
    boolean existsByUserId(String userId);

    /**
     * Find all user IDs that have push notifications disabled.
     */
    @Query("SELECT p.user.id FROM NotificationPreferences p WHERE p.pushEnabled = false")
    List<String> findUserIdsWithPushDisabled();
}
