package com.extractor.unraveldocs.documents.utils;

import org.springframework.stereotype.Component;

@Component
public class SanitizeLogging {
    public String sanitizeLogging(String input) {
        if (input == null) {
            return "null";
        }

        return input.replaceAll("[^a-zA-Z0-9_\\-.]", "_")
                   .replaceAll("_+", "_")
                   .replaceAll("^_|_$", "");
    }
}
