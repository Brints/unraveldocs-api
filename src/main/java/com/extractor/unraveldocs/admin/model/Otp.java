package com.extractor.unraveldocs.admin.model;

import com.extractor.unraveldocs.user.model.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

@Data
@Entity
@Table(name = "admin_otp")
@NoArgsConstructor
@AllArgsConstructor
public class Otp {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", referencedColumnName = "id", nullable = false)
    private User user;

    @Column(nullable = false, name = "otp_code", unique = true)
    private String otpCode;

    @Column(nullable = false, columnDefinition = "TIMESTAMP", name = "expires_at")
    private OffsetDateTime expiresAt;

    @Column(nullable = false, name = "is_used")
    private boolean isUsed = false;

    @Column(name = "is_expired", nullable = false)
    private boolean isExpired = false;

    @CreationTimestamp
    @Column(nullable = false, updatable = false, name = "created_at")
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false, name = "updated_at")
    private OffsetDateTime updatedAt;
}
