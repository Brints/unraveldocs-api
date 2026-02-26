package com.extractor.unraveldocs.ratelimit.interceptor;

import com.extractor.unraveldocs.ratelimit.service.RateLimitService;
import com.extractor.unraveldocs.subscription.datamodel.SubscriptionPlans;
import com.extractor.unraveldocs.subscription.model.UserSubscription;
import com.extractor.unraveldocs.subscription.repository.UserSubscriptionRepository;
import com.extractor.unraveldocs.user.model.User;
import com.extractor.unraveldocs.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Optional;

/**
 * Interceptor that enforces rate limits on AI endpoints.
 * Extracts the authenticated user, resolves their subscription tier,
 * and delegates to RateLimitService for token bucket enforcement.
 *
 * Adds standard rate-limit response headers:
 * <ul>
 * <li>X-RateLimit-Limit — max requests per minute for the user's tier</li>
 * <li>X-RateLimit-Remaining — tokens remaining in the per-minute bucket</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimitService rateLimitService;
    private final UserRepository userRepository;
    private final UserSubscriptionRepository userSubscriptionRepository;

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull Object handler) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            // Let the security filter chain handle unauthenticated requests
            return true;
        }

        String email = auth.getName();
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            return true;
        }

        User user = userOpt.get();
        SubscriptionPlans plan = resolveUserPlan(user.getId());

        long remaining = rateLimitService.consumeToken(user.getId(), plan);
        int limit = rateLimitService.getLimit(plan);

        // Add rate-limit headers
        response.setHeader("X-RateLimit-Limit", String.valueOf(limit));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(remaining));

        return true;
    }

    /**
     * Resolve the user's current subscription plan.
     * Returns null (treated as FREE) if no subscription exists.
     */
    private SubscriptionPlans resolveUserPlan(String userId) {
        Optional<UserSubscription> subscription = userSubscriptionRepository.findByUserIdWithPlan(userId);
        if (subscription.isEmpty() || subscription.get().getPlan() == null) {
            return null;
        }
        return subscription.get().getPlan().getName();
    }
}
