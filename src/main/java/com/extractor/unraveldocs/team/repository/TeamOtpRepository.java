package com.extractor.unraveldocs.team.repository;

import com.extractor.unraveldocs.team.model.TeamOtpVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;

@Repository
public interface TeamOtpRepository extends JpaRepository<TeamOtpVerification, String> {

    @Query("SELECT o FROM TeamOtpVerification o WHERE o.userId = :userId AND o.otp = :otp AND o.expiresAt > :now AND o.isUsed = false")
    Optional<TeamOtpVerification> findValidOtp(@Param("userId") String userId, @Param("otp") String otp,
            @Param("now") OffsetDateTime now);

    @Query("SELECT o FROM TeamOtpVerification o WHERE o.userId = :userId AND o.isUsed = false ORDER BY o.createdAt DESC")
    Optional<TeamOtpVerification> findLatestUnusedOtpByUserId(@Param("userId") String userId);

    @Modifying
    @Query("DELETE FROM TeamOtpVerification o WHERE o.expiresAt < :now OR o.isUsed = true")
    int cleanupExpiredOrUsedOtps(@Param("now") OffsetDateTime now);

    @Modifying
    @Query("UPDATE TeamOtpVerification o SET o.isUsed = true WHERE o.userId = :userId")
    int markAllOtpsAsUsedForUser(@Param("userId") String userId);

    long countByUserIdAndIsUsedFalseAndExpiresAtAfter(String userId, OffsetDateTime now);
}
