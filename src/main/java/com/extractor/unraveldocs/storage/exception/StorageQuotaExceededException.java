package com.extractor.unraveldocs.storage.exception;

import com.extractor.unraveldocs.exceptions.custom.BadRequestException;
import lombok.Getter;

/**
 * Exception thrown when a user's storage quota has been exceeded.
 */
@Getter
public class StorageQuotaExceededException extends BadRequestException {

    private final long requiredBytes;
    private final long availableBytes;
    private final long storageLimit;

    public StorageQuotaExceededException(long requiredBytes, long availableBytes, long storageLimit) {
        super(buildMessage(requiredBytes, availableBytes, storageLimit));
        this.requiredBytes = requiredBytes;
        this.availableBytes = availableBytes;
        this.storageLimit = storageLimit;
    }

    public StorageQuotaExceededException(String message) {
        super(message);
        this.requiredBytes = 0;
        this.availableBytes = 0;
        this.storageLimit = 0;
    }

    private static String buildMessage(long requiredBytes, long availableBytes, long storageLimit) {
        return String.format(
                "Storage quota exceeded. Required: %s, Available: %s, Limit: %s. Please upgrade your plan or delete some files.",
                formatBytes(requiredBytes),
                formatBytes(availableBytes),
                formatBytes(storageLimit));
    }

    private static String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", bytes / (1024.0 * 1024));
        } else {
            return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
        }
    }

}
