package com.extractor.unraveldocs.credit.repository;

import com.extractor.unraveldocs.credit.model.UserCreditBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

@Repository
public interface UserCreditBalanceRepository extends JpaRepository<UserCreditBalance, String> {

    Optional<UserCreditBalance> findByUserId(String userId);

    // --- Admin Stats Aggregation Queries ---

    @Query("SELECT COALESCE(SUM(b.balance), 0) FROM UserCreditBalance b")
    long sumTotalBalance();

    @Query("SELECT COALESCE(SUM(b.totalPurchased), 0) FROM UserCreditBalance b")
    long sumTotalPurchased();

    @Query("SELECT COALESCE(SUM(b.totalUsed), 0) FROM UserCreditBalance b")
    long sumTotalUsed();

    @Query("SELECT COUNT(b) FROM UserCreditBalance b WHERE b.balance = 0")
    long countUsersWithZeroBalance();

    @Query("SELECT COALESCE(AVG(CAST(b.balance AS double)), 0.0) FROM UserCreditBalance b")
    BigDecimal averageCreditBalance();
}
