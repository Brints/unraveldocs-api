package com.extractor.unraveldocs.documents.datamodel;

public enum DocumentUploadState {
    SUCCESS("success"),
    FAILED_VALIDATION("failed_validation"),
    FAILED_STORAGE("failed_storage"),
    FAILED_STORAGE_UPLOAD("failed_storage_upload");

    private final String state;
    DocumentUploadState(String state) {
        this.state = state;
    }
    @Override
    public String toString() {
        return state;
    }
}
