package com.extractor.unraveldocs.pushnotification.repository;

import com.extractor.unraveldocs.pushnotification.model.StorageWarningSent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for StorageWarningSent entity operations.
 */
@Repository
public interface StorageWarningSentRepository extends JpaRepository<StorageWarningSent, String> {

    /**
     * Find all warnings sent to a user.
     */
    List<StorageWarningSent> findByUserId(String userId);

    /**
     * Check if a specific warning level was already sent to a user.
     */
    boolean existsByUserIdAndWarningLevel(String userId, Integer warningLevel);

    /**
     * Find a specific warning record.
     */
    Optional<StorageWarningSent> findByUserIdAndWarningLevel(String userId, Integer warningLevel);

    /**
     * Delete all warning records for a user (used when storage is cleared).
     */
    @Modifying
    @Query("DELETE FROM StorageWarningSent s WHERE s.user.id = :userId")
    int deleteAllForUser(@Param("userId") String userId);

    /**
     * Delete warning records below a certain level for a user.
     * Used when user frees up storage and drops below a threshold.
     */
    @Modifying
    @Query("DELETE FROM StorageWarningSent s WHERE s.user.id = :userId AND s.warningLevel > :level")
    int deleteAboveLevel(@Param("userId") String userId, @Param("level") Integer level);
}
