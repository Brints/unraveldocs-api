package com.extractor.unraveldocs.documents.datamodel;

public enum DocumentStatus {
    UPLOADED("uploaded"),
    COMPLETED("completed"),
    PARTIALLY_COMPLETED("partially_completed"),
    FAILED_UPLOAD("failed_upload"),
    PROCESSING("processing"),
    PROCESSED("processed"),
    FAILED_OCR("failed_ocr"),
    DELETED("deleted");

    private final String documentStatus;

    DocumentStatus(String documentStatus) {
        this.documentStatus = documentStatus;
    }

    @Override
    public String toString() {
        return documentStatus;
    }
}
