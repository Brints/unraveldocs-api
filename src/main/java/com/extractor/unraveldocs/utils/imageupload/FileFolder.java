package com.extractor.unraveldocs.utils.imageupload;

import lombok.Getter;

@Getter
public enum FileFolder {
    PROFILE_PICTURE("profile_pictures/"),
    DOCUMENT_PICTURE("documents/"),
    RECEIPT("receipts/");

    private final String folder;

    FileFolder(String folder) {
        this.folder = folder;
    }
}
