package com.extractor.unraveldocs.utils.imageupload;

import lombok.Getter;

@Getter
public enum FileSize {
    SINGLE_FILE_SIZE(10 * 1024 * 1024), // 10 MB
    MULTIPLE_FILE_SIZE(50 * 1024 * 1024); // 50 MB

    private final long size;

    FileSize(long size) {
        this.size = size;
    }

    public static boolean isValidFileSize(long fileSize, boolean isMultipleFiles) {
        long maxSize = isMultipleFiles ? MULTIPLE_FILE_SIZE.getSize() : SINGLE_FILE_SIZE.getSize();
        return fileSize <= maxSize;
    }

    public static String getFileSizeLimitMessage(boolean isMultipleFiles) {
        return isMultipleFiles ?
            "The total size of all files must not exceed " + MULTIPLE_FILE_SIZE.getSize() / (1024 * 1024) + " MB." :
            "The file size must not exceed " + SINGLE_FILE_SIZE.getSize() / (1024 * 1024) + " MB.";
    }
}
