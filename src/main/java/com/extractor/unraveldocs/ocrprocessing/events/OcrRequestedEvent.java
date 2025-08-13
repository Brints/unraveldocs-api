package com.extractor.unraveldocs.ocrprocessing.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OcrRequestedEvent implements Serializable {
    private String collectionId;
    private String documentId;
}
