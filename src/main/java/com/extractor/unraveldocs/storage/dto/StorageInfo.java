package com.extractor.unraveldocs.storage.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * DTO containing storage, OCR, and document usage information for a user or
 * team.
 *
 * <p>Monthly quotas (documents uploaded, OCR pages used) reset on the first day of each month.
 * Storage usage is cumulative and does not reset.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StorageInfo {
    // Storage fields (cumulative, not reset monthly)
    private Long storageUsed;
    private Long storageLimit; // null = unlimited
    private String storageUsedFormatted;
    private String storageLimitFormatted;
    private Double percentageUsed;
    private boolean isUnlimited;

    // OCR fields (resets monthly)
    private Integer ocrPageLimit; // null = unlimited
    private Integer ocrPagesUsed;
    private Integer ocrPagesRemaining;
    private boolean ocrUnlimited;

    // Document upload fields (resets monthly)
    private Integer documentUploadLimit; // null = unlimited
    private Integer documentsUploaded;  // Monthly count, resets on quotaResetDate
    private Integer documentsRemaining;
    private boolean documentsUnlimited;

    // Subscription plan info
    private String subscriptionPlan;
    private String billingInterval;

    // Quota reset info
    private OffsetDateTime quotaResetDate; // When monthly quotas (documents, OCR pages) will reset

    /**
     * Check if storage quota is exceeded.
     */
    public boolean isQuotaExceeded() {
        if (isUnlimited || storageLimit == null) {
            return false;
        }
        return storageUsed >= storageLimit;
    }

    /**
     * Get remaining storage in bytes.
     */
    public Long getRemainingStorage() {
        if (isUnlimited || storageLimit == null) {
            return null;
        }
        return Math.max(0, storageLimit - storageUsed);
    }

    /**
     * Check if OCR quota is exceeded.
     */
    public boolean isOcrQuotaExceeded() {
        if (ocrUnlimited || ocrPageLimit == null) {
            return false;
        }
        return ocrPagesUsed != null && ocrPagesUsed >= ocrPageLimit;
    }

    /**
     * Check if document upload quota is exceeded.
     */
    public boolean isDocumentQuotaExceeded() {
        if (documentsUnlimited || documentUploadLimit == null) {
            return false;
        }
        return documentsUploaded != null && documentsUploaded >= documentUploadLimit;
    }
}
