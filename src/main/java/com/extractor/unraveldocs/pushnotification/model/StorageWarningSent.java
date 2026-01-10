package com.extractor.unraveldocs.pushnotification.model;

import com.extractor.unraveldocs.user.model.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

/**
 * Entity to track storage warning notifications sent to users.
 * Prevents sending duplicate warnings for the same threshold.
 */
@Data
@Entity
@Table(name = "storage_warning_sent", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "user_id", "warning_level" })
})
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StorageWarningSent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "warning_level", nullable = false)
    private Integer warningLevel; // 80, 90, or 95

    @CreationTimestamp
    @Column(name = "sent_at", nullable = false)
    private OffsetDateTime sentAt;
}
