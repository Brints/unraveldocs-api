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
}