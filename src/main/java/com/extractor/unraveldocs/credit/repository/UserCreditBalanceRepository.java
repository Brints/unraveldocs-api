package com.extractor.unraveldocs.credit.repository;

import com.extractor.unraveldocs.credit.model.UserCreditBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserCreditBalanceRepository extends JpaRepository<UserCreditBalance, String> {

    Optional<UserCreditBalance> findByUserId(String userId);
}
