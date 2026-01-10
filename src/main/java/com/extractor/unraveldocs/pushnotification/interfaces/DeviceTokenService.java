package com.extractor.unraveldocs.pushnotification.interfaces;

import com.extractor.unraveldocs.pushnotification.dto.request.RegisterDeviceRequest;
import com.extractor.unraveldocs.pushnotification.dto.response.DeviceTokenResponse;
import com.extractor.unraveldocs.pushnotification.model.UserDeviceToken;

import java.util.List;

/**
 * Service interface for managing device tokens.
 */
public interface DeviceTokenService {

    /**
     * Register a new device token for a user.
     */
    DeviceTokenResponse registerDevice(String userId, RegisterDeviceRequest request);

    /**
     * Unregister a device token.
     */
    void unregisterDevice(String userId, String tokenId);

    /**
     * Unregister a device by token string.
     */
    void unregisterByToken(String deviceToken);

    /**
     * Get all active device tokens for a user.
     */
    List<DeviceTokenResponse> getActiveDevices(String userId);

    /**
     * Get all device tokens for a user (including inactive).
     */
    List<DeviceTokenResponse> getAllDevices(String userId);

    /**
     * Get device token entities for a user (internal use).
     */
    List<UserDeviceToken> getActiveTokenEntities(String userId);

    /**
     * Update the last used timestamp for a device token.
     */
    void updateLastUsed(String deviceToken);

    /**
     * Deactivate all devices for a user.
     */
    void deactivateAllDevices(String userId);

    /**
     * Check if a device token is valid/active.
     */
    boolean isTokenActive(String deviceToken);
}
