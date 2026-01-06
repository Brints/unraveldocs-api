package com.extractor.unraveldocs.storage.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO containing storage usage information for a user or team.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StorageInfo {
    private Long storageUsed;
    private Long storageLimit; // null = unlimited
    private String storageUsedFormatted;
    private String storageLimitFormatted;
    private Double percentageUsed;
    private boolean isUnlimited;

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
}
