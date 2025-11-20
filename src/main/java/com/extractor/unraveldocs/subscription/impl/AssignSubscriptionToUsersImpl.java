package com.extractor.unraveldocs.subscription.impl;

import com.extractor.unraveldocs.shared.response.ResponseBuilderService;
import com.extractor.unraveldocs.shared.response.UnravelDocsResponse;
import com.extractor.unraveldocs.subscription.dto.response.AllSubscriptionPlans;
import com.extractor.unraveldocs.subscription.interfaces.AssignSubscriptionToUsersService;
import com.extractor.unraveldocs.subscription.model.UserSubscription;
import com.extractor.unraveldocs.user.model.User;
import com.extractor.unraveldocs.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class AssignSubscriptionToUsersImpl implements AssignSubscriptionToUsersService {
    private final AssignSubscriptionService assignSubscriptionService;
    private final ResponseBuilderService responseBuilderService;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public UnravelDocsResponse<AllSubscriptionPlans> assignSubscriptionsToExistingUsers() {
        List<User> usersWithoutSubscription = userRepository.findBySubscriptionIsNull();
        AtomicInteger assignedCount = new AtomicInteger(0);

        usersWithoutSubscription.forEach(user -> {
            UserSubscription subscription = assignSubscriptionService.assignDefaultSubscription(user);
            if (subscription != null) {
                user.setSubscription(subscription);
                userRepository.save(user);
                assignedCount.incrementAndGet();
                log.info("Assigned default subscription to user: {}", user.getEmail());
            }
        });

        String message = String.format(
                "Successfully assigned subscriptions to %d users.",
                assignedCount.get()
        );

        AllSubscriptionPlans data = new AllSubscriptionPlans();
        data.setAssignedCount(assignedCount.get());

        return responseBuilderService.buildUserResponse(data, HttpStatus.OK, message);
    }
}
