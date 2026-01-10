package com.extractor.unraveldocs.pushnotification.repository;

import com.extractor.unraveldocs.pushnotification.datamodel.DeviceType;
import com.extractor.unraveldocs.pushnotification.model.UserDeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for UserDeviceToken entity operations.
 */
@Repository
public interface DeviceTokenRepository extends JpaRepository<UserDeviceToken, String> {

    /**
     * Find all active device tokens for a user.
     */
    List<UserDeviceToken> findByUserIdAndIsActiveTrue(String userId);

    /**
     * Find a device token by the token string.
     */
    Optional<UserDeviceToken> findByDeviceToken(String deviceToken);

    /**
     * Find a device token by user and token.
     */
    Optional<UserDeviceToken> findByUserIdAndDeviceToken(String userId, String deviceToken);

    /**
     * Find all device tokens by user.
     */
    List<UserDeviceToken> findByUserId(String userId);

    /**
     * Find all active tokens by device type for a user.
     */
    List<UserDeviceToken> findByUserIdAndDeviceTypeAndIsActiveTrue(String userId, DeviceType deviceType);

    /**
     * Count active devices for a user.
     */
    long countByUserIdAndIsActiveTrue(String userId);

    /**
     * Deactivate all tokens for a user.
     */
    @Modifying
    @Query("UPDATE UserDeviceToken t SET t.isActive = false WHERE t.user.id = :userId")
    int deactivateAllForUser(@Param("userId") String userId);

    /**
     * Delete inactive tokens older than a certain date.
     */
    @Modifying
    @Query("DELETE FROM UserDeviceToken t WHERE t.isActive = false AND t.updatedAt < :cutoffDate")
    int deleteInactiveOlderThan(@Param("cutoffDate") java.time.OffsetDateTime cutoffDate);

    /**
     * Check if a device token exists.
     */
    boolean existsByDeviceToken(String deviceToken);
}
