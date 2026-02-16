package com.extractor.unraveldocs.utils.imageupload;

import lombok.Getter;

@Getter
public enum FileType {
    IMAGE("image/gif", "image/jpeg", "image/png", "image/jpg"),
    VIDEO("video/mp4", "video/quicktime", "video/x-msvideo", "video/x-flv", "video/webm", "video/ogg"),
    DOCUMENT("application/pdf", "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation"),
    AUDIO("audio/mpeg", "audio/wav", "audio/ogg", "audio/aac"),
    FILE("image/gif", "image/jpeg", "image/png", "image/jpg",
            "application/pdf", "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation");

    private final String[] mimeTypes;

    FileType(String... mimeTypes) {
        this.mimeTypes = mimeTypes;
    }

    public boolean isValid(String mimeType) {
        for (String type : mimeTypes) {
            if (type.equalsIgnoreCase(mimeType)) {
                return true;
            }
        }
        return false;
    }
}
