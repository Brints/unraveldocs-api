package com.extractor.unraveldocs.admin.service.impl;

import com.extractor.unraveldocs.admin.dto.response.UserListData;
import com.extractor.unraveldocs.admin.dto.response.UserSummary;
import com.extractor.unraveldocs.admin.interfaces.AdminPlanSubscribersService;
import com.extractor.unraveldocs.shared.response.UnravelDocsResponse;
import com.extractor.unraveldocs.subscription.model.UserSubscription;
import com.extractor.unraveldocs.subscription.repository.UserSubscriptionRepository;
import com.extractor.unraveldocs.user.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminPlanSubscribersServiceImpl implements AdminPlanSubscribersService {

    private final UserSubscriptionRepository userSubscriptionRepository;

    @Override
    @Transactional(readOnly = true)
    public UnravelDocsResponse<UserListData> getPlanSubscribers(String planId, int page, int size) {
        log.info("Fetching subscribers for plan ID: {}, page: {}, size: {}", planId, page, size);

        // Sanitize pagination inputs
        int p = Math.max(0, page);
        int s = size > 0 ? size : 10;
        Pageable pageable = PageRequest.of(p, s);

        Page<UserSubscription> subscriptionsPage = userSubscriptionRepository.findByPlanIdWithUser(planId, pageable);

        List<UserSummary> userSummaries = subscriptionsPage.stream()
                .map(sub -> mapToUserSummary(sub.getUser()))
                .collect(Collectors.toList());

        UserListData listData = new UserListData();
        listData.setUsers(userSummaries);
        listData.setTotalUsers((int) subscriptionsPage.getTotalElements());
        listData.setTotalPages(subscriptionsPage.getTotalPages());
        listData.setCurrentPage(subscriptionsPage.getNumber());
        listData.setPageSize(subscriptionsPage.getSize());

        return new UnravelDocsResponse<>(200, "success", "Plan subscribers retrieved successfully", listData);
    }

    private UserSummary mapToUserSummary(User user) {
        UserSummary summary = new UserSummary();
        summary.setId(user.getId());
        summary.setProfilePicture(user.getProfilePicture());
        summary.setFirstName(user.getFirstName());
        summary.setLastName(user.getLastName());
        summary.setEmail(user.getEmail());
        summary.setRole(user.getRole());
        summary.setActive(user.isActive());
        summary.setVerified(user.isVerified());
        summary.setLastLogin(user.getLastLogin());
        summary.setCreatedAt(user.getCreatedAt());
        summary.setUpdatedAt(user.getUpdatedAt());
        return summary;
    }
}
