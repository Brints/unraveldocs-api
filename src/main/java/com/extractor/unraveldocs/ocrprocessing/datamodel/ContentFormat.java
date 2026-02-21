package com.extractor.unraveldocs.ocrprocessing.datamodel;

import lombok.Getter;

@Getter
public enum ContentFormat {
    HTML("html"),
    MARKDOWN("markdown");

    private final String format;

    ContentFormat(String format) {
        this.format = format;
    }
}
