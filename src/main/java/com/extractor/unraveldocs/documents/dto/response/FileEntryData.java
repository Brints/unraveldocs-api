package com.extractor.unraveldocs.documents.dto.response;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileEntryData {
    private String documentId;
    private String originalFileName;
    private long fileSize;
    private String fileUrl;
    private String status;
}