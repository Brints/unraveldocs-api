package com.extractor.unraveldocs.loginattempts.repository;

import com.extractor.unraveldocs.loginattempts.model.LoginAttempts;
import com.extractor.unraveldocs.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LoginAttemptsRepository extends JpaRepository<LoginAttempts, String> {
    Optional<LoginAttempts> findByUser(User user);
}
