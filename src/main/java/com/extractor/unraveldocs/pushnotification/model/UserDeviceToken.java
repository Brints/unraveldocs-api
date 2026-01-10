package com.extractor.unraveldocs.pushnotification.model;

import com.extractor.unraveldocs.pushnotification.datamodel.DeviceType;
import com.extractor.unraveldocs.user.model.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

/**
 * Entity representing a device token for push notifications.
 * Each user can have multiple devices registered.
 */
@Data
@Entity
@Table(name = "user_device_tokens", indexes = {
        @Index(columnList = "user_id"),
        @Index(columnList = "is_active")
})
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDeviceToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "device_token", nullable = false, unique = true, length = 512)
    private String deviceToken;

    @Enumerated(EnumType.STRING)
    @Column(name = "device_type", nullable = false, length = 20)
    private DeviceType deviceType;

    @Column(name = "device_name", length = 100)
    private String deviceName;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "last_used_at")
    private OffsetDateTime lastUsedAt;

    /**
     * Update the last used timestamp.
     */
    public void updateLastUsed() {
        this.lastUsedAt = OffsetDateTime.now();
    }

    /**
     * Deactivate this device token.
     */
    public void deactivate() {
        this.isActive = false;
    }
}
