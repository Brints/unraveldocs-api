package com.extractor.unraveldocs.admin.config;

import com.extractor.unraveldocs.auth.datamodel.Role;
import com.extractor.unraveldocs.auth.datamodel.VerifiedStatus;
import com.extractor.unraveldocs.auth.model.UserVerification;
import com.extractor.unraveldocs.loginattempts.model.LoginAttempts;
import com.extractor.unraveldocs.subscription.datamodel.BillingIntervalUnit;
import com.extractor.unraveldocs.subscription.datamodel.SubscriptionCurrency;
import com.extractor.unraveldocs.subscription.datamodel.SubscriptionPlans;
import com.extractor.unraveldocs.subscription.impl.AssignSubscriptionService;
import com.extractor.unraveldocs.subscription.model.SubscriptionPlan;
import com.extractor.unraveldocs.subscription.repository.SubscriptionPlanRepository;
import com.extractor.unraveldocs.user.model.User;
import com.extractor.unraveldocs.user.repository.UserRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    @Value("${app.admin.email:#{null}}")
    private String adminEmail;

    @Value("${app.admin.password:#{null}}")
    private String adminPassword;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        if (adminEmail == null || adminPassword == null) {
            log.warn("Admin email or password not set in application properties. Skipping admin user creation.");
            return;
        }

        User adminUser = null;
        if (userRepository.existsByEmail(adminEmail)) {
            log.info("Admin user already exists with email: {}", adminEmail);
        } else {
            log.info("Creating admin user with email: {}", adminEmail);
            adminUser = createAdminUser();
            userRepository.save(adminUser);
            log.info("Admin user has been created: {}", adminUser);
        }

        createDefaultSubscriptionPlans();

        if (adminUser != null && adminUser.getSubscription() == null) {
            var subscription = subscriptionService.assignDefaultSubscription(adminUser);
            adminUser.setSubscription(subscription);
            userRepository.save(adminUser);
            log.info("Default subscription assigned to admin user: {}", adminEmail);
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
                plan.setPrice(BigDecimal.ZERO);
                plan.setBillingIntervalUnit(BillingIntervalUnit.MONTH);
                plan.setBillingIntervalValue(1);
                plan.setDocumentUploadLimit(10);
                plan.setOcrPageLimit(50);
                break;
            case BASIC_MONTHLY:
                plan.setPrice(new BigDecimal("13.00"));
                plan.setBillingIntervalUnit(BillingIntervalUnit.MONTH);
                plan.setBillingIntervalValue(1);
                plan.setDocumentUploadLimit(50);
                plan.setOcrPageLimit(250);
                break;
            case BASIC_YEARLY:
                plan.setPrice(new BigDecimal("100.00"));
                plan.setBillingIntervalUnit(BillingIntervalUnit.YEAR);
                plan.setBillingIntervalValue(1);
                plan.setDocumentUploadLimit(600);
                plan.setOcrPageLimit(3000);
                break;
            case PREMIUM_MONTHLY:
                plan.setPrice(new BigDecimal("28.00"));
                plan.setBillingIntervalUnit(BillingIntervalUnit.MONTH);
                plan.setBillingIntervalValue(1);
                plan.setDocumentUploadLimit(200);
                plan.setOcrPageLimit(1000);
                break;
            case PREMIUM_YEARLY:
                plan.setPrice(new BigDecimal("250.00"));
                plan.setBillingIntervalUnit(BillingIntervalUnit.YEAR);
                plan.setBillingIntervalValue(1);
                plan.setDocumentUploadLimit(2400);
                plan.setOcrPageLimit(12000);
                break;
            case ENTERPRISE_MONTHLY:
                plan.setPrice(new BigDecimal("100.00"));
                plan.setBillingIntervalUnit(BillingIntervalUnit.MONTH);
                plan.setBillingIntervalValue(1);
                plan.setDocumentUploadLimit(1000);
                plan.setOcrPageLimit(5000);
                break;
            case ENTERPRISE_YEARLY:
                plan.setPrice(new BigDecimal("1000.00"));
                plan.setBillingIntervalUnit(BillingIntervalUnit.YEAR);
                plan.setBillingIntervalValue(1);
                plan.setDocumentUploadLimit(12000);
                plan.setOcrPageLimit(60000);
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
