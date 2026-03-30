package com.extractor.unraveldocs.admin.repository;

import com.extractor.unraveldocs.admin.dto.request.UserFilterDto;
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
    public Page<User> findAllUsers(UserFilterDto filter, Pageable pageable) {
        StringBuilder selectStr = new StringBuilder("SELECT DISTINCT u FROM User u ");
        StringBuilder countSelectStr = new StringBuilder("SELECT COUNT(DISTINCT u) FROM User u ");
        
        StringBuilder joins = new StringBuilder();
        StringBuilder where = new StringBuilder(" WHERE u.deletedAt IS NULL ");

        // Complex Joins based on filters
        if (filter.getSubscriptionPlan() != null) {
            joins.append(" LEFT JOIN u.subscription sub LEFT JOIN sub.plan p ");
            where.append(" AND p.name = :subscriptionPlan");
        }
        if (filter.getIsBlocked() != null) {
            joins.append(" LEFT JOIN u.loginAttempts la ");
            where.append(" AND la.isBlocked = :isBlocked");
        }
        if (filter.getHasTeam() != null) {
            if (filter.getHasTeam()) {
                where.append(" AND EXISTS (SELECT 1 FROM TeamMember tm WHERE tm.user = u) ");
            } else {
                where.append(" AND NOT EXISTS (SELECT 1 FROM TeamMember tm WHERE tm.user = u) ");
            }
        }

        // Basic Filters
        if (filter.getSearch() != null && !filter.getSearch().trim().isEmpty()) {
            where.append(" AND (LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%'))")
                 .append(" OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :search, '%'))")
                 .append(" OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')))");
        }
        if (filter.getFirstName() != null) where.append(" AND u.firstName = :firstName");
        if (filter.getLastName() != null) where.append(" AND u.lastName = :lastName");
        if (filter.getEmail() != null) where.append(" AND u.email = :email");
        if (filter.getRole() != null) where.append(" AND u.role = :role");
        if (filter.getIsActive() != null) where.append(" AND u.isActive = :isActive");
        if (filter.getIsVerified() != null) where.append(" AND u.isVerified = :isVerified");
        if (filter.getIsPlatformAdmin() != null) where.append(" AND u.isPlatformAdmin = :isPlatformAdmin");
        if (filter.getIsOrganizationAdmin() != null) where.append(" AND u.isOrganizationAdmin = :isOrganizationAdmin");
        if (filter.getCountry() != null) where.append(" AND u.country = :country");
        if (filter.getProfession() != null) where.append(" AND u.profession = :profession");
        if (filter.getOrganization() != null) where.append(" AND u.organization = :organization");
        if (filter.getCreatedAfter() != null) where.append(" AND u.createdAt >= :createdAfter");
        if (filter.getCreatedBefore() != null) where.append(" AND u.createdAt <= :createdBefore");
        if (filter.getLastLoginAfter() != null) where.append(" AND u.lastLogin >= :lastLoginAfter");
        if (filter.getLastLoginBefore() != null) where.append(" AND u.lastLogin <= :lastLoginBefore");

        // Order By
        StringBuilder orderBy = new StringBuilder();
        if (pageable.getSort().isSorted()) {
            orderBy.append(" ORDER BY ");
            pageable.getSort().forEach(order -> {
                orderBy.append("u.").append(order.getProperty()).append(" ").append(order.getDirection().name()).append(", ");
            });
            orderBy.setLength(orderBy.length() - 2); // remove last comma
        }

        // Create Queries
        String fullDataQuery = selectStr.toString() + joins.toString() + where.toString() + orderBy.toString();
        String fullCountQuery = countSelectStr.toString() + joins.toString() + where.toString();

        TypedQuery<User> query = entityManager.createQuery(fullDataQuery, User.class);
        TypedQuery<Long> countQuery = entityManager.createQuery(fullCountQuery, Long.class);

        // Set parameters
        if (filter.getSubscriptionPlan() != null) {
            query.setParameter("subscriptionPlan", filter.getSubscriptionPlan().name());
            countQuery.setParameter("subscriptionPlan", filter.getSubscriptionPlan().name());
        }
        if (filter.getIsBlocked() != null) {
            query.setParameter("isBlocked", filter.getIsBlocked());
            countQuery.setParameter("isBlocked", filter.getIsBlocked());
        }

        if (filter.getSearch() != null && !filter.getSearch().trim().isEmpty()) {
            query.setParameter("search", filter.getSearch().trim());
            countQuery.setParameter("search", filter.getSearch().trim());
        }
        if (filter.getFirstName() != null) { query.setParameter("firstName", filter.getFirstName()); countQuery.setParameter("firstName", filter.getFirstName()); }
        if (filter.getLastName() != null) { query.setParameter("lastName", filter.getLastName()); countQuery.setParameter("lastName", filter.getLastName()); }
        if (filter.getEmail() != null) { query.setParameter("email", filter.getEmail()); countQuery.setParameter("email", filter.getEmail()); }
        if (filter.getRole() != null) { query.setParameter("role", filter.getRole()); countQuery.setParameter("role", filter.getRole()); }
        if (filter.getIsActive() != null) { query.setParameter("isActive", filter.getIsActive()); countQuery.setParameter("isActive", filter.getIsActive()); }
        if (filter.getIsVerified() != null) { query.setParameter("isVerified", filter.getIsVerified()); countQuery.setParameter("isVerified", filter.getIsVerified()); }
        if (filter.getIsPlatformAdmin() != null) { query.setParameter("isPlatformAdmin", filter.getIsPlatformAdmin()); countQuery.setParameter("isPlatformAdmin", filter.getIsPlatformAdmin()); }
        if (filter.getIsOrganizationAdmin() != null) { query.setParameter("isOrganizationAdmin", filter.getIsOrganizationAdmin()); countQuery.setParameter("isOrganizationAdmin", filter.getIsOrganizationAdmin()); }
        if (filter.getCountry() != null) { query.setParameter("country", filter.getCountry()); countQuery.setParameter("country", filter.getCountry()); }
        if (filter.getProfession() != null) { query.setParameter("profession", filter.getProfession()); countQuery.setParameter("profession", filter.getProfession()); }
        if (filter.getOrganization() != null) { query.setParameter("organization", filter.getOrganization()); countQuery.setParameter("organization", filter.getOrganization()); }
        if (filter.getCreatedAfter() != null) { query.setParameter("createdAfter", filter.getCreatedAfter()); countQuery.setParameter("createdAfter", filter.getCreatedAfter()); }
        if (filter.getCreatedBefore() != null) { query.setParameter("createdBefore", filter.getCreatedBefore()); countQuery.setParameter("createdBefore", filter.getCreatedBefore()); }
        if (filter.getLastLoginAfter() != null) { query.setParameter("lastLoginAfter", filter.getLastLoginAfter()); countQuery.setParameter("lastLoginAfter", filter.getLastLoginAfter()); }
        if (filter.getLastLoginBefore() != null) { query.setParameter("lastLoginBefore", filter.getLastLoginBefore()); countQuery.setParameter("lastLoginBefore", filter.getLastLoginBefore()); }

        Long total = countQuery.getSingleResult();

        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());

        List<User> users = query.getResultList();

        return new PageImpl<>(users, pageable, total);
    }
}

