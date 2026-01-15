package com.extractor.unraveldocs.documents.model;

import com.extractor.unraveldocs.documents.datamodel.DocumentStatus;
import com.extractor.unraveldocs.user.model.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "document_collections")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentCollection {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
    private User user;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "document_file_entries", joinColumns = @JoinColumn(name = "document_collection_id"))
    @Builder.Default
    private List<FileEntry> files = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "collection_status", length = 50)
    private DocumentStatus collectionStatus;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "upload_timestamp", nullable = false)
    private OffsetDateTime uploadTimestamp;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}