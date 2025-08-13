package com.extractor.unraveldocs.admin.repository;

import com.extractor.unraveldocs.auth.datamodel.Role;
import com.extractor.unraveldocs.user.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CustomUserRepository {
    Page<User> findAllUsers(String searchTerm, String firstName, String lastName, String email, Role role, Boolean isActive, Boolean isVerified, Pageable pageable);
}
