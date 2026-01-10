package com.extractor.unraveldocs.pushnotification.controller;

import com.extractor.unraveldocs.pushnotification.datamodel.NotificationType;
import com.extractor.unraveldocs.pushnotification.dto.request.RegisterDeviceRequest;
import com.extractor.unraveldocs.pushnotification.dto.request.UpdatePreferencesRequest;
import com.extractor.unraveldocs.pushnotification.dto.response.DeviceTokenResponse;
import com.extractor.unraveldocs.pushnotification.dto.response.NotificationPreferencesResponse;
import com.extractor.unraveldocs.pushnotification.dto.response.NotificationResponse;
import com.extractor.unraveldocs.pushnotification.interfaces.DeviceTokenService;
import com.extractor.unraveldocs.pushnotification.interfaces.NotificationPreferencesService;
import com.extractor.unraveldocs.pushnotification.interfaces.NotificationService;
import com.extractor.unraveldocs.user.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for push notification management.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Push notification management endpoints")
public class NotificationController {

    private final NotificationService notificationService;
    private final DeviceTokenService deviceTokenService;
    private final NotificationPreferencesService preferencesService;

    // ==================== Device Token Endpoints ====================

    @PostMapping("/device")
    @Operation(summary = "Register device token", description = "Register a device for push notifications")
    public ResponseEntity<DeviceTokenResponse> registerDevice(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody RegisterDeviceRequest request) {

        DeviceTokenResponse response = deviceTokenService.registerDevice(user.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/device/{tokenId}")
    @Operation(summary = "Unregister device", description = "Unregister a device from push notifications")
    public ResponseEntity<Void> unregisterDevice(
            @AuthenticationPrincipal User user,
            @PathVariable String tokenId) {

        deviceTokenService.unregisterDevice(user.getId(), tokenId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/devices")
    @Operation(summary = "Get registered devices", description = "Get all registered devices for the user")
    public ResponseEntity<List<DeviceTokenResponse>> getDevices(
            @AuthenticationPrincipal User user) {

        List<DeviceTokenResponse> devices = deviceTokenService.getActiveDevices(user.getId());
        return ResponseEntity.ok(devices);
    }

    // ==================== Notification Endpoints ====================

    @GetMapping
    @Operation(summary = "Get notifications", description = "Get paginated list of notifications")
    public ResponseEntity<Page<NotificationResponse>> getNotifications(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<NotificationResponse> notifications = notificationService.getUserNotifications(user.getId(), pageable);
        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/unread")
    @Operation(summary = "Get unread notifications", description = "Get unread notifications")
    public ResponseEntity<Page<NotificationResponse>> getUnreadNotifications(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<NotificationResponse> notifications = notificationService.getUnreadNotifications(user.getId(), pageable);
        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/by-type/{type}")
    @Operation(summary = "Get notifications by type", description = "Get notifications filtered by type")
    public ResponseEntity<Page<NotificationResponse>> getNotificationsByType(
            @AuthenticationPrincipal User user,
            @PathVariable NotificationType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<NotificationResponse> notifications = notificationService.getNotificationsByType(user.getId(), type,
                pageable);
        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Get unread count", description = "Get the count of unread notifications")
    public ResponseEntity<Map<String, Long>> getUnreadCount(
            @AuthenticationPrincipal User user) {

        long count = notificationService.getUnreadCount(user.getId());
        return ResponseEntity.ok(Map.of("count", count));
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Mark as read", description = "Mark a notification as read")
    public ResponseEntity<Void> markAsRead(
            @AuthenticationPrincipal User user,
            @PathVariable String id) {

        notificationService.markAsRead(user.getId(), id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/read-all")
    @Operation(summary = "Mark all as read", description = "Mark all notifications as read")
    public ResponseEntity<Void> markAllAsRead(
            @AuthenticationPrincipal User user) {

        notificationService.markAllAsRead(user.getId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete notification", description = "Delete a notification")
    public ResponseEntity<Void> deleteNotification(
            @AuthenticationPrincipal User user,
            @PathVariable String id) {

        notificationService.deleteNotification(user.getId(), id);
        return ResponseEntity.noContent().build();
    }

    // ==================== Preferences Endpoints ====================

    @GetMapping("/preferences")
    @Operation(summary = "Get preferences", description = "Get notification preferences")
    public ResponseEntity<NotificationPreferencesResponse> getPreferences(
            @AuthenticationPrincipal User user) {

        NotificationPreferencesResponse preferences = preferencesService.getPreferences(user.getId());
        return ResponseEntity.ok(preferences);
    }

    @PutMapping("/preferences")
    @Operation(summary = "Update preferences", description = "Update notification preferences")
    public ResponseEntity<NotificationPreferencesResponse> updatePreferences(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody UpdatePreferencesRequest request) {

        NotificationPreferencesResponse preferences = preferencesService.updatePreferences(user.getId(), request);
        return ResponseEntity.ok(preferences);
    }
}
