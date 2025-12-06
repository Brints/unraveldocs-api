package com.extractor.unraveldocs.admin.repository;

import com.extractor.unraveldocs.admin.model.Otp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OtpRepository extends JpaRepository<Otp, String> {
    Optional<Otp> findByOtpCodeAndIsExpiredFalseAndIsUsedFalse(String otpCode);

    // find active OTPs
    @Query("SELECT o FROM Otp o WHERE o.isExpired = false AND o.isUsed = false ORDER BY o.createdAt DESC")
    List<Otp> findActiveOtps();

    @Query("SELECT o FROM Otp o WHERE o.isExpired = false AND o.isUsed = false ORDER BY o.createdAt DESC")
    org.springframework.data.domain.Page<Otp> findActiveOtps(org.springframework.data.domain.Pageable pageable);

    /**
     * Bulk update to mark OTPs as expired when their expiry time has passed
     * @param now Current timestamp
     * @return Number of OTPs marked as expired
     */
    @Modifying
    @Query("UPDATE Otp o SET o.isExpired = true, o.updatedAt = :now WHERE o.expiresAt < :now AND o.isExpired = false")
    int markExpiredOtps(@Param("now") OffsetDateTime now);

    /**
     * Bulk delete OTPs that have been expired for more than the specified threshold
     * @param threshold Timestamp threshold (e.g., 24 hours ago)
     * @return Number of OTPs deleted
     */
    @Modifying
    @Query("DELETE FROM Otp o WHERE o.isExpired = true AND o.updatedAt < :threshold")
    int deleteExpiredOtpsOlderThan(@Param("threshold") OffsetDateTime threshold);
}
