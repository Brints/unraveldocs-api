package com.extractor.unraveldocs.loginattempts.repository;

import com.extractor.unraveldocs.loginattempts.model.LoginAttempts;
import com.extractor.unraveldocs.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LoginAttemptsRepository extends JpaRepository<LoginAttempts, String> {
    Optional<LoginAttempts> findByUser(User user);

    long countByIsBlockedTrue();

    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(l.loginAttempts), 0) FROM LoginAttempts l WHERE l.updatedAt >= :since")
    long sumFailedLoginsSince(@org.springframework.data.repository.query.Param("since") java.time.LocalDateTime since);
}
