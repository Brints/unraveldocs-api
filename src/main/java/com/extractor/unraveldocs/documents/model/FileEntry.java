package com.extractor.unraveldocs.documents.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileEntry {
    @Builder.Default
    @Column(name = "document_id", nullable = false, unique = true)
    private String documentId = UUID.randomUUID().toString();

    @Column(name = "original_file_name", nullable = false)
    private String originalFileName;

    @Column(name = "storage_id")
    private String storageId;

    @Column(name = "file_url", length = 1024)
    private String fileUrl;

    @Column(name = "file_type", nullable = false)
    private String fileType;

    @Column(name = "file_size", nullable = false)
    private long fileSize;

    @Column(name = "upload_status", nullable = false)
    private String uploadStatus;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "is_encrypted")
    @Builder.Default
    private boolean isEncrypted = false;

    @Column(name = "encryption_iv")
    private String encryptionIv;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @UpdateTimestamp
    @Column(name = "updated_at")
    @Builder.Default
    private OffsetDateTime updatedAt = OffsetDateTime.now();
}