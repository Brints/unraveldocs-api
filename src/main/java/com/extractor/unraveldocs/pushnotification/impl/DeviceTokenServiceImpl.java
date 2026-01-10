package com.extractor.unraveldocs.pushnotification.impl;

import com.extractor.unraveldocs.pushnotification.config.NotificationConfig;
import com.extractor.unraveldocs.pushnotification.dto.request.RegisterDeviceRequest;
import com.extractor.unraveldocs.pushnotification.dto.response.DeviceTokenResponse;
import com.extractor.unraveldocs.pushnotification.interfaces.DeviceTokenService;
import com.extractor.unraveldocs.pushnotification.model.UserDeviceToken;
import com.extractor.unraveldocs.pushnotification.repository.DeviceTokenRepository;
import com.extractor.unraveldocs.user.model.User;
import com.extractor.unraveldocs.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Implementation of DeviceTokenService.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceTokenServiceImpl implements DeviceTokenService {

    private final DeviceTokenRepository deviceTokenRepository;
    private final UserRepository userRepository;
    private final NotificationConfig notificationConfig;

    @Override
    @Transactional
    public DeviceTokenResponse registerDevice(String userId, RegisterDeviceRequest request) {
        log.debug("Registering device for user {}: {}", userId, request.getDeviceType());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        // Check if token already exists
        Optional<UserDeviceToken> existingToken = deviceTokenRepository
                .findByDeviceToken(request.getDeviceToken());

        if (existingToken.isPresent()) {
            UserDeviceToken token = existingToken.get();
            // If token belongs to another user, transfer it
            if (!token.getUser().getId().equals(userId)) {
                token.setUser(user);
                log.info("Device token transferred to user {}", userId);
            }
            // Reactivate if inactive
            token.setActive(true);
            token.setDeviceName(request.getDeviceName());
            token.updateLastUsed();
            UserDeviceToken saved = deviceTokenRepository.save(token);
            return mapToResponse(saved);
        }

        // Check max devices limit
        long currentDevices = deviceTokenRepository.countByUserIdAndIsActiveTrue(userId);
        if (currentDevices >= notificationConfig.getMaxDevicesPerUser()) {
            log.warn("User {} has reached max devices limit ({})", userId, notificationConfig.getMaxDevicesPerUser());
            throw new IllegalStateException("Maximum number of devices reached. Please remove a device first.");
        }

        // Create new device token
        UserDeviceToken newToken = UserDeviceToken.builder()
                .user(user)
                .deviceToken(request.getDeviceToken())
                .deviceType(request.getDeviceType())
                .deviceName(request.getDeviceName())
                .isActive(true)
                .build();

        UserDeviceToken saved = deviceTokenRepository.save(newToken);
        log.info("New device registered for user {}: {}", userId, saved.getId());
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public void unregisterDevice(String userId, String tokenId) {
        deviceTokenRepository.findById(tokenId)
                .filter(token -> token.getUser().getId().equals(userId))
                .ifPresent(token -> {
                    token.deactivate();
                    deviceTokenRepository.save(token);
                    log.info("Device {} unregistered for user {}", tokenId, userId);
                });
    }

    @Override
    @Transactional
    public void unregisterByToken(String deviceToken) {
        deviceTokenRepository.findByDeviceToken(deviceToken)
                .ifPresent(token -> {
                    token.deactivate();
                    deviceTokenRepository.save(token);
                    log.info("Device token unregistered: {}", token.getId());
                });
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeviceTokenResponse> getActiveDevices(String userId) {
        return deviceTokenRepository.findByUserIdAndIsActiveTrue(userId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeviceTokenResponse> getAllDevices(String userId) {
        return deviceTokenRepository.findByUserId(userId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDeviceToken> getActiveTokenEntities(String userId) {
        return deviceTokenRepository.findByUserIdAndIsActiveTrue(userId);
    }

    @Override
    @Transactional
    public void updateLastUsed(String deviceToken) {
        deviceTokenRepository.findByDeviceToken(deviceToken)
                .ifPresent(token -> {
                    token.updateLastUsed();
                    deviceTokenRepository.save(token);
                });
    }

    @Override
    @Transactional
    public void deactivateAllDevices(String userId) {
        int deactivated = deviceTokenRepository.deactivateAllForUser(userId);
        log.info("Deactivated {} devices for user {}", deactivated, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isTokenActive(String deviceToken) {
        return deviceTokenRepository.findByDeviceToken(deviceToken)
                .map(UserDeviceToken::isActive)
                .orElse(false);
    }

    private DeviceTokenResponse mapToResponse(UserDeviceToken token) {
        return DeviceTokenResponse.builder()
                .id(token.getId())
                .deviceToken(maskToken(token.getDeviceToken()))
                .deviceType(token.getDeviceType())
                .deviceName(token.getDeviceName())
                .isActive(token.isActive())
                .createdAt(token.getCreatedAt())
                .lastUsedAt(token.getLastUsedAt())
                .build();
    }

    private String maskToken(String token) {
        if (token == null || token.length() < 8) {
            return "****";
        }
        return token.substring(0, 4) + "****" + token.substring(token.length() - 4);
    }
}
