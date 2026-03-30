package com.extractor.unraveldocs.admin.service.impl;

import com.extractor.unraveldocs.admin.dto.response.UserListData;
import com.extractor.unraveldocs.shared.response.UnravelDocsResponse;
import com.extractor.unraveldocs.subscription.datamodel.SubscriptionPlans;
import com.extractor.unraveldocs.subscription.model.SubscriptionPlan;
import com.extractor.unraveldocs.subscription.model.UserSubscription;
import com.extractor.unraveldocs.subscription.repository.UserSubscriptionRepository;
import com.extractor.unraveldocs.user.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminPlanSubscribersServiceImplTest {

    @Mock
    private UserSubscriptionRepository userSubscriptionRepository;

    @InjectMocks
    private AdminPlanSubscribersServiceImpl testClass;

    private UserSubscription mockSubscription;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setId("user-1");
        user.setEmail("user1@example.com");
        user.setFirstName("John");
        user.setLastName("Doe");

        SubscriptionPlan plan = new SubscriptionPlan();
        plan.setId("plan-1");
        plan.setName(SubscriptionPlans.PRO_MONTHLY);

        mockSubscription = new UserSubscription();
        mockSubscription.setId("sub-1");
        mockSubscription.setUser(user);
        mockSubscription.setPlan(plan);
    }

    @Test
    void getPlanSubscribers_success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<UserSubscription> page = new PageImpl<>(List.of(mockSubscription), pageable, 1);

        when(userSubscriptionRepository.findByPlanIdWithUser(eq("plan-1"), any(Pageable.class)))
                .thenReturn(page);

        UnravelDocsResponse<UserListData> response = testClass.getPlanSubscribers("plan-1", 0, 10);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode());

        UserListData data = response.getData();
        assertEquals(1, data.getTotalUsers());
        assertEquals(1, data.getUsers().size());
        assertEquals("user-1", data.getUsers().getFirst().getId());
        assertEquals("user1@example.com", data.getUsers().getFirst().getEmail());
    }

    @Test
    void getPlanSubscribers_handlesNegativePage() {
        Page<UserSubscription> page = new PageImpl<>(List.of());

        when(userSubscriptionRepository.findByPlanIdWithUser(eq("plan-1"), any(Pageable.class)))
                .thenReturn(page);

        UnravelDocsResponse<UserListData> response = testClass.getPlanSubscribers("plan-1", -5, 10);

        assertEquals(200, response.getStatusCode());
        assertEquals(0, response.getData().getTotalUsers());
    }
}
