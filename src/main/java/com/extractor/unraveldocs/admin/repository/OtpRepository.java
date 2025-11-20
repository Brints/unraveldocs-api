package com.extractor.unraveldocs.admin.repository;

import com.extractor.unraveldocs.admin.model.Otp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OtpRepository extends JpaRepository<Otp, String> {
}
