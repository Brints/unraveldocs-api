package com.extractor.unraveldocs.admin.config;

import com.extractor.unraveldocs.auth.datamodel.Role;
import com.extractor.unraveldocs.auth.datamodel.VerifiedStatus;
import com.extractor.unraveldocs.auth.model.UserVerification;
import com.extractor.unraveldocs.credit.datamodel.CreditPackName;
import com.extractor.unraveldocs.credit.model.CreditPack;
import com.extractor.unraveldocs.credit.repository.CreditPackRepository;
import com.extractor.unraveldocs.credit.repository.UserCreditBalanceRepository;
import com.extractor.unraveldocs.credit.service.CreditBalanceService;
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
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminUserInitializer implements CommandLineRunner {

    private final AssignSubscriptionService subscriptionService;
    private final CreditPackRepository creditPackRepository;
    private final CreditBalanceService creditBalanceService;
    private final UserCreditBalanceRepository creditBalanceRepository;
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
        createDefaultCreditPacks();

        User adminUser = null;
        if (userRepository.existsByEmail(adminEmail)) {
            log.info("Admin user already exists with email: {}", adminEmail);
            adminUser = userRepository.findByEmail(adminEmail).orElse(null);
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

        // Assign unlimited credits to admins and free credits to regular users
        initializeUserCredits(adminUser);
    }

    /**
     * Single-pass credit initialization for all users:
     * - Admin/Super Admin: unlimited credits
     * - Regular users without a balance: 5 free signup bonus credits
     */
    private void initializeUserCredits(User primaryAdmin) {
        try {
            List<User> allUsers = userRepository.findAll();
            int adminCount = 0;
            int bonusCount = 0;

            for (User user : allUsers) {
                boolean isAdmin = user.getRole() == Role.ADMIN || user.getRole() == Role.SUPER_ADMIN;

                if (isAdmin) {
                    creditBalanceService.grantUnlimitedCredits(user);
                    adminCount++;
                } else {
                    boolean hasBalance = creditBalanceRepository.findByUserId(user.getId()).isPresent();
                    if (!hasBalance) {
                        creditBalanceService.grantSignupBonus(user);
                        bonusCount++;
                    }
                }
            }

            if (adminCount > 0) {
                log.info("Unlimited credits assigned to {} admin/super admin user(s)", adminCount);
            }
            if (bonusCount > 0) {
                log.info("Granted free signup bonus credits to {} existing user(s)", bonusCount);
            }
        } catch (Exception e) {
            log.error("Failed to grant free credits to existing users: {}", e.getMessage());
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
                plan.setDocumentUploadLimit(5);
                plan.setOcrPageLimit(25);
                plan.setStorageLimit(120L * 1024 * 1024);
                break;
            case STARTER_MONTHLY:
                plan.setPrice(new BigDecimal("9.00"));
                plan.setBillingIntervalUnit(BillingIntervalUnit.MONTH);
                plan.setBillingIntervalValue(1);
                plan.setDocumentUploadLimit(30);
                plan.setOcrPageLimit(150);
                plan.setStorageLimit((long) (2.6 * 1024 * 1024 * 1024));
                break;
            case STARTER_YEARLY:
                plan.setPrice(new BigDecimal("90.00"));
                plan.setBillingIntervalUnit(BillingIntervalUnit.YEAR);
                plan.setBillingIntervalValue(1);
                plan.setDocumentUploadLimit(360);
                plan.setOcrPageLimit(1800);
                plan.setStorageLimit((long) (2.6 * 1024 * 1024 * 1024));
                break;
            case PRO_MONTHLY:
                plan.setPrice(new BigDecimal("19.00"));
                plan.setBillingIntervalUnit(BillingIntervalUnit.MONTH);
                plan.setBillingIntervalValue(1);
                plan.setDocumentUploadLimit(100);
                plan.setOcrPageLimit(500);
                plan.setStorageLimit((long) (12.3 * 1024 * 1024 * 1024));
                break;
            case PRO_YEARLY:
                plan.setPrice(new BigDecimal("190.00"));
                plan.setBillingIntervalUnit(BillingIntervalUnit.YEAR);
                plan.setBillingIntervalValue(1);
                plan.setDocumentUploadLimit(1200);
                plan.setOcrPageLimit(6000);
                plan.setStorageLimit((long) (12.3 * 1024 * 1024 * 1024));
                break;
            case BUSINESS_MONTHLY:
                plan.setPrice(new BigDecimal("49.00"));
                plan.setBillingIntervalUnit(BillingIntervalUnit.MONTH);
                plan.setBillingIntervalValue(1);
                plan.setDocumentUploadLimit(500);
                plan.setOcrPageLimit(2500);
                plan.setStorageLimit(30L * 1024 * 1024 * 1024);
                break;
            case BUSINESS_YEARLY:
                plan.setPrice(new BigDecimal("490.00"));
                plan.setBillingIntervalUnit(BillingIntervalUnit.YEAR);
                plan.setBillingIntervalValue(1);
                plan.setDocumentUploadLimit(6000);
                plan.setOcrPageLimit(30000);
                plan.setStorageLimit(30L * 1024 * 1024 * 1024);
                break;
        }
        return plan;
    }

    private void createDefaultCreditPacks() {
        EnumSet.allOf(CreditPackName.class).forEach(packName -> {
            boolean exists = creditPackRepository.findByName(packName).isPresent();
            if (!exists) {
                CreditPack newPack = createDefaultCreditPack(packName);
                creditPackRepository.save(newPack);
                log.info("Created default credit pack: {}", packName.getDisplayName());
            }
        });
    }

    private CreditPack createDefaultCreditPack(CreditPackName packName) {
        CreditPack pack = new CreditPack();
        pack.setName(packName);
        pack.setDisplayName(packName.getDisplayName());
        pack.setCurrency("USD");
        pack.setIsActive(true);

        switch (packName) {
            case STARTER_PACK:
                pack.setPriceInCents(500L);
                pack.setCreditsIncluded(20);
                break;
            case VALUE_PACK:
                pack.setPriceInCents(1500L);
                pack.setCreditsIncluded(75);
                break;
            case POWER_PACK:
                pack.setPriceInCents(3000L);
                pack.setCreditsIncluded(200);
                break;
        }
        return pack;
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
