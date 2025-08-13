package com.extractor.unraveldocs.user.repository;

import com.extractor.unraveldocs.auth.datamodel.Role;
import com.extractor.unraveldocs.user.model.User;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByEmail(String email);

    Optional<User> findUserById(String id);

    List<User> findBySubscriptionIsNull();

    boolean existsByEmail(String email);

    @Query("SELECT u FROM User u WHERE " +
            "(:search IS NULL OR " +
            "LOWER(CAST(u.firstName AS string)) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) OR " +
            "LOWER(CAST(u.lastName AS string)) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) OR " +
            "LOWER(CAST(u.email AS string)) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))) AND " +
            "(:firstName IS NULL OR LOWER(CAST(u.firstName AS string)) LIKE LOWER(CONCAT('%', CAST(:firstName AS string), '%'))) AND " +
            "(:lastName IS NULL OR LOWER(CAST(u.lastName AS string)) LIKE LOWER(CONCAT('%', CAST(:lastName AS string), '%'))) AND " +
            "(:email IS NULL OR LOWER(CAST(u.email AS string)) LIKE LOWER(CONCAT('%', CAST(:email AS string), '%'))) AND " +
            "(:role IS NULL OR u.role = :role) AND " +
            "(:isActive IS NULL OR u.isActive = :isActive) AND " +
            "(:isVerified IS NULL OR u.isVerified = :isVerified) AND " +
            "u.deletedAt IS NULL")
    Page<User> findAllUsers(
            @Param("search") String search,
            @Param("firstName") String firstName,
            @Param("lastName") String lastName,
            @Param("email") String email,
            @Param("role") Role role,
            @Param("isActive") Boolean isActive,
            @Param("isVerified") Boolean isVerified,
            Pageable pageable
    );

    @Cacheable("superAdminExists")
    @Query("SELECT COUNT(u) = 0 FROM User u WHERE u.role = com.extractor.unraveldocs.auth.datamodel.Role.SUPER_ADMIN")
    boolean superAdminExists();

    @Query("SELECT u FROM User u WHERE u.lastLogin < :threshold AND u.deletedAt IS NULL")
    Page<User> findAllByLastLoginDateBefore(@Param("threshold") OffsetDateTime threshold, Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.deletedAt < :threshold")
    Page<User> findAllByDeletedAtBefore(@Param("threshold") OffsetDateTime threshold, Pageable pageable);
}




//    @Query("SELECT u FROM User u WHERE u.lastLogin < :threshold AND u.deletedAt IS NULL")
//    List<User> findAllByDeletedAtBefore(@Param("threshold") OffsetDateTime threshold);