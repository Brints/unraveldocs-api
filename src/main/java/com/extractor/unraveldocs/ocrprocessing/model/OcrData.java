package com.extractor.unraveldocs.ocrprocessing.model;

import com.extractor.unraveldocs.ocrprocessing.datamodel.OcrStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "ocr_data")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OcrData {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "document_id", nullable = false, unique = true)
    private String documentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OcrStatus status;

    @Lob
    @Column(name = "extracted_text", columnDefinition = "TEXT")
    private String extractedText;

    @Column(name = "error_message")
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
