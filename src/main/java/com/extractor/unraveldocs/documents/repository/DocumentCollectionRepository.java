package com.extractor.unraveldocs.documents.repository;

import com.extractor.unraveldocs.documents.model.DocumentCollection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DocumentCollectionRepository extends JpaRepository<DocumentCollection, String> {
    @Query("SELECT dc FROM DocumentCollection dc WHERE dc.user.id = :userId ORDER BY dc.createdAt DESC")
    List<DocumentCollection> findAllByUserId(@Param("userId") String userId);

    @Query("SELECT COUNT(dc) FROM DocumentCollection dc WHERE dc.user.id = :userId")
    Long countByUserId(@Param("userId") String userId);

    @Modifying
    @Query("DELETE FROM DocumentCollection dc WHERE dc.user.id = :userId")
    void deleteAllByUserId(@Param("userId") String userId);

    // --- Admin Stats Aggregation Queries ---

    @Query("SELECT COUNT(f) FROM DocumentCollection c JOIN c.files f")
    long countTotalFiles();

    @Query("SELECT COALESCE(SUM(f.fileSize), 0) FROM DocumentCollection c JOIN c.files f")
    long sumTotalStorageBytes();

    @Query("SELECT f.fileType, COUNT(f) FROM DocumentCollection c JOIN c.files f GROUP BY f.fileType")
    List<Object[]> countFilesByType();

    @Query("SELECT f.uploadStatus, COUNT(f) FROM DocumentCollection c JOIN c.files f GROUP BY f.uploadStatus")
    List<Object[]> countFilesByStatus();

    @Query("SELECT COUNT(f) FROM DocumentCollection c JOIN c.files f WHERE f.isEncrypted = true")
    long countEncryptedDocuments();
}