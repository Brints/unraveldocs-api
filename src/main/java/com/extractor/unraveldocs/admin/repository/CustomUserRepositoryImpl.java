package com.extractor.unraveldocs.admin.repository;

import com.extractor.unraveldocs.auth.datamodel.Role;
import com.extractor.unraveldocs.user.model.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class CustomUserRepositoryImpl implements CustomUserRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Page<User> findAllUsers(String searchTerm, String firstName, String lastName, String email, Role role, Boolean isActive, Boolean isVerified, Pageable pageable) {

        StringBuilder queryStr = new StringBuilder("SELECT u FROM User u WHERE 1=1");

        // Add filters dynamically
        if (searchTerm != null && !searchTerm.isEmpty()) {
            queryStr.append(" AND (LOWER(u.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
                    .append(" OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
                    .append(" OR LOWER(u.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')))");
        }
        if (firstName != null) queryStr.append(" AND u.firstName = :firstName");
        if (lastName != null) queryStr.append(" AND u.lastName = :lastName");
        if (email != null) queryStr.append(" AND u.email = :email");
        if (role != null) queryStr.append(" AND u.role = :role");
        if (isActive != null) queryStr.append(" AND u.isActive = :isActive");
        if (isVerified != null) queryStr.append(" AND u.isVerified = :isVerified");

        TypedQuery<User> query = entityManager.createQuery(queryStr.toString(), User.class);

        // Set parameters
        if (searchTerm != null && !searchTerm.isEmpty()) query.setParameter("searchTerm", searchTerm);
        if (firstName != null) query.setParameter("firstName", firstName);
        if (lastName != null) query.setParameter("lastName", lastName);
        if (email != null) query.setParameter("email", email);
        if (role != null) query.setParameter("role", role);
        if (isActive != null) query.setParameter("isActive", isActive);
        if (isVerified != null) query.setParameter("isVerified", isVerified);

        int total = query.getResultList().size(); // Not efficient for large datasets; consider a count query
        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());

        List<User> users = query.getResultList();

        return new PageImpl<>(users, pageable, total);
    }
}

