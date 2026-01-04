package com.extractor.unraveldocs.admin.config;

import com.extractor.unraveldocs.auth.datamodel.Role;
import com.extractor.unraveldocs.auth.datamodel.VerifiedStatus;
import com.extractor.unraveldocs.auth.model.UserVerification;
import com.extractor.unraveldocs.documents.utils.SanitizeLogging;
import com.extractor.unraveldocs.loginattempts.model.LoginAttempts;
import com.extractor.unraveldocs.subscription.datamodel.BillingIntervalUnit;
import com.extractor.unraveldocs.subscription.datamodel.SubscriptionCurrency;
import com.extractor.unraveldocs.subscription.datamodel.SubscriptionPlans;
import com.extractor.unraveldocs.subscription.impl.AssignSubscriptionService;
import com.extractor.unraveldocs.subscription.model.SubscriptionPlan;
import com.extractor.unraveldocs.subscription.repository.SubscriptionPlanRepository;
import com.extractor.unraveldocs.user.model.User;
import com.extractor.unraveldocs.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.EnumSet;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminUserInitializer implements CommandLineRunner {

    private final AssignSubscriptionService subscriptionService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SubscriptionPlanRepository planRepository;
    private final SanitizeLogging sanitizer;

    @Value("${app.admin.email:#{null}}")
    private String adminEmail;

    @Value("${app.admin.password:#{null}}")
    private String adminPassword;

    @Override
    @Transactional
    public void run(String @NonNull... args) throws Exception {
        if (adminEmail == null || adminPassword == null) {
            log.warn("Admin email or password not set in application properties. Skipping admin user creation.");
            return;
        }

        // Create plans FIRST so they exist when assigning subscriptions
        createDefaultSubscriptionPlans();

        User adminUser = null;
        if (userRepository.existsByEmail(adminEmail)) {
            log.info("Admin user already exists with email: {}", adminEmail);
        } else {
            log.info("Creating admin user with email: {}", adminEmail);
            adminUser = createAdminUser();
            userRepository.save(adminUser);
            log.info("Admin user has been created: {}", adminUser);
        }

        if (adminUser != null && adminUser.getSubscription() == null) {
            var subscription = subscriptionService.assignDefaultSubscription(adminUser);
            adminUser.setSubscription(subscription);
            userRepository.save(adminUser);
            log.info("Default subscription assigned to admin user: {}", sanitizer.sanitizeLogging(adminEmail));
        }
    }

    private User createAdminUser() {
        OffsetDateTime now = OffsetDateTime.now();

        var adminUser = new User();
        adminUser.setFirstName("Admin");
        adminUser.setLastName("User");
        adminUser.setEmail(adminEmail);
        adminUser.setPassword(passwordEncoder.encode(adminPassword));
        adminUser.setLastLogin(null);
        adminUser.setActive(true);
        adminUser.setVerified(true);
        adminUser.setRole(Role.SUPER_ADMIN);
        adminUser.setPlatformAdmin(true);
        adminUser.setCountry("NG");
        adminUser.setTermsAccepted(true);
        adminUser.setMarketingOptIn(true);
        adminUser.setCreatedAt(now);
        adminUser.setUpdatedAt(now);

        var verification = getVerification(adminUser, now);
        adminUser.setUserVerification(verification);

        var loginAttempts = new LoginAttempts();
        loginAttempts.setUser(adminUser);
        adminUser.setLoginAttempts(loginAttempts);

        return adminUser;
    }

    private void createDefaultSubscriptionPlans() {
        EnumSet.allOf(SubscriptionPlans.class).forEach(planEnum -> {
            if (planRepository.findByName(planEnum).isEmpty()) {
                SubscriptionPlan newPlan = createPlanFromEnum(planEnum);
                planRepository.save(newPlan);
                log.info("Created default subscription plan: {}", planEnum.getPlanName());
            }
        });
    }

    private SubscriptionPlan createPlanFromEnum(SubscriptionPlans planEnum) {
        SubscriptionPlan plan = new SubscriptionPlan();
        plan.setName(planEnum);
        plan.setActive(true);
        plan.setCurrency(SubscriptionCurrency.USD);

        switch (planEnum) {
            case FREE:
                // Free tier - limited features for trial/evaluation
                plan.setPrice(BigDecimal.ZERO);
                plan.setBillingIntervalUnit(BillingIntervalUnit.MONTH);
                plan.setBillingIntervalValue(1);
                plan.setDocumentUploadLimit(5);
                plan.setOcrPageLimit(25);
                break;
            case STARTER_MONTHLY:
                // Starter tier - for individual light users ($9/mo)
                plan.setPrice(new BigDecimal("9.00"));
                plan.setBillingIntervalUnit(BillingIntervalUnit.MONTH);
                plan.setBillingIntervalValue(1);
                plan.setDocumentUploadLimit(30);
                plan.setOcrPageLimit(150);
                break;
            case STARTER_YEARLY:
                // Starter yearly - 17% savings ($90/year = $7.50/mo)
                plan.setPrice(new BigDecimal("90.00"));
                plan.setBillingIntervalUnit(BillingIntervalUnit.YEAR);
                plan.setBillingIntervalValue(1);
                plan.setDocumentUploadLimit(360);
                plan.setOcrPageLimit(1800);
                break;
            case PRO_MONTHLY:
                // Pro tier - for power users ($19/mo)
                plan.setPrice(new BigDecimal("19.00"));
                plan.setBillingIntervalUnit(BillingIntervalUnit.MONTH);
                plan.setBillingIntervalValue(1);
                plan.setDocumentUploadLimit(100);
                plan.setOcrPageLimit(500);
                break;
            case PRO_YEARLY:
                // Pro yearly - 17% savings ($190/year = $15.83/mo)
                plan.setPrice(new BigDecimal("190.00"));
                plan.setBillingIntervalUnit(BillingIntervalUnit.YEAR);
                plan.setBillingIntervalValue(1);
                plan.setDocumentUploadLimit(1200);
                plan.setOcrPageLimit(6000);
                break;
            case BUSINESS_MONTHLY:
                // Business tier - for heavy users ($49/mo)
                plan.setPrice(new BigDecimal("49.00"));
                plan.setBillingIntervalUnit(BillingIntervalUnit.MONTH);
                plan.setBillingIntervalValue(1);
                plan.setDocumentUploadLimit(500);
                plan.setOcrPageLimit(2500);
                break;
            case BUSINESS_YEARLY:
                // Business yearly - 17% savings ($490/year = $40.83/mo)
                plan.setPrice(new BigDecimal("490.00"));
                plan.setBillingIntervalUnit(BillingIntervalUnit.YEAR);
                plan.setBillingIntervalValue(1);
                plan.setDocumentUploadLimit(6000);
                plan.setOcrPageLimit(30000);
                break;
        }
        return plan;
    }

    private static UserVerification getVerification(User adminUser, OffsetDateTime now) {
        var verification = new UserVerification();
        verification.setUser(adminUser);
        verification.setEmailVerified(true);
        verification.setStatus(VerifiedStatus.VERIFIED);
        verification.setDeletedAt(null);
        verification.setCreatedAt(now);
        verification.setUpdatedAt(now);
        return verification;
    }
}
