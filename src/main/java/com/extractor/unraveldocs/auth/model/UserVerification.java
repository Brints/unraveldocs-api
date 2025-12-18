package com.extractor.unraveldocs.auth.model;

import com.extractor.unraveldocs.auth.datamodel.VerifiedStatus;
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
@Table(name = "user_verification", indexes = {
        @Index(columnList = "email_verification_token"),
        @Index(columnList = "password_reset_token")
})
@NoArgsConstructor
@AllArgsConstructor
public class UserVerification {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @OneToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
    private User user;

    @Column
    private boolean emailVerified = false;

    @Column
    private String emailVerificationToken;

    @Enumerated(EnumType.STRING)
    @Column
    private VerifiedStatus status = VerifiedStatus.PENDING;

    @Column(columnDefinition = "TIMESTAMP")
    private OffsetDateTime emailVerificationTokenExpiry;

    @Column
    private String passwordResetToken;

    @Column(columnDefinition = "TIMESTAMP")
    private OffsetDateTime passwordResetTokenExpiry;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    @CreationTimestamp
    @Column(
            nullable = false,
            updatable = false,
            name = "created_at"
    )
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(
            nullable = false,
            name = "updated_at"
    )
    private OffsetDateTime updatedAt;
}
