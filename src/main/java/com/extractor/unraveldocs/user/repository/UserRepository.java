package com.extractor.unraveldocs.user.repository;

import com.extractor.unraveldocs.auth.datamodel.Role;
import com.extractor.unraveldocs.user.model.User;
import lombok.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import com.extractor.unraveldocs.admin.repository.CustomUserRepository;

public interface UserRepository extends JpaRepository<@NonNull User, @NonNull String>, CustomUserRepository {
        Optional<User> findByEmail(String email);

        Optional<User> findUserById(String id);

        List<User> findBySubscriptionIsNull();

        boolean existsByEmail(String email);

        @Query("SELECT u FROM User u WHERE u.lastLogin < :threshold AND u.deletedAt IS NULL")
        Page<User> findAllByLastLoginDateBefore(@Param("threshold") OffsetDateTime threshold, Pageable pageable);

        @Query("SELECT u FROM User u WHERE u.deletedAt < :threshold")
        Page<User> findAllByDeletedAtBefore(@Param("threshold") OffsetDateTime threshold, Pageable pageable);

        // ========== Coupon Notification Query Methods (Performance Optimized)
        // ==========

        /**
         * Find users created after a specific date who are not deleted.
         * Used for targeting new users with welcome coupons.
         */
        List<User> findByCreatedAtAfterAndDeletedAtIsNull(OffsetDateTime createdAfter);

        /**
         * Find all non-deleted users.
         * Used for targeting all users with coupons.
         */
        List<User> findByDeletedAtIsNull();

        // ========== Dashboard KPI Query Methods ==========

        long countByDeletedAtIsNull();

        long countByIsActiveTrueAndIsVerifiedTrueAndDeletedAtIsNull();

        long countByIsVerifiedTrueAndDeletedAtIsNull();

        long countByIsVerifiedFalseAndDeletedAtIsNull();

        long countByCreatedAtAfterAndDeletedAtIsNull(OffsetDateTime createdAt);

        long countByLastLoginAfterAndDeletedAtIsNull(OffsetDateTime lastLogin);

        // ========== User Statistics Aggregation Queries ==========

        long countByIsActiveTrueAndDeletedAtIsNull();

        long countByIsActiveFalseAndDeletedAtIsNull();

        long countByDeletedAtIsNotNull();

        long countByMarketingOptInTrueAndDeletedAtIsNull();

        @Query("SELECT u.role, COUNT(u) FROM User u WHERE u.deletedAt IS NULL GROUP BY u.role")
        List<Object[]> countByRoleGrouped();

        @Query("SELECT u.country, COUNT(u) FROM User u WHERE u.deletedAt IS NULL GROUP BY u.country ORDER BY COUNT(u) DESC")
        List<Object[]> countByCountryGrouped();

        @Query("SELECT u.profession, COUNT(u) FROM User u WHERE u.deletedAt IS NULL AND u.profession IS NOT NULL GROUP BY u.profession ORDER BY COUNT(u) DESC")
        List<Object[]> countByProfessionGrouped();

        @Query("SELECT u.organization, COUNT(u) FROM User u WHERE u.deletedAt IS NULL AND u.organization IS NOT NULL GROUP BY u.organization ORDER BY COUNT(u) DESC")
        List<Object[]> countByOrganizationGrouped();

        @Query("SELECT CAST(u.createdAt AS DATE), COUNT(u) FROM User u WHERE u.createdAt >= :since AND u.deletedAt IS NULL GROUP BY CAST(u.createdAt AS DATE) ORDER BY CAST(u.createdAt AS DATE)")
        List<Object[]> countNewUsersPerDaySince(@Param("since") OffsetDateTime since);
}
