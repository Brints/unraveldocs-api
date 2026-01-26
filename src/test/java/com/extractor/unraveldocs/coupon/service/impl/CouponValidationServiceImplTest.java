package com.extractor.unraveldocs.coupon.service.impl;

import com.extractor.unraveldocs.auth.datamodel.Role;
import com.extractor.unraveldocs.coupon.dto.CouponData;
import com.extractor.unraveldocs.coupon.dto.request.ApplyCouponRequest;
import com.extractor.unraveldocs.coupon.dto.response.CouponValidationResponse;
import com.extractor.unraveldocs.coupon.dto.response.DiscountCalculationData;
import com.extractor.unraveldocs.coupon.enums.RecipientCategory;
import com.extractor.unraveldocs.coupon.helpers.CouponMapper;
import com.extractor.unraveldocs.coupon.model.Coupon;
import com.extractor.unraveldocs.coupon.repository.CouponRecipientRepository;
import com.extractor.unraveldocs.coupon.repository.CouponRepository;
import com.extractor.unraveldocs.coupon.repository.CouponUsageRepository;
import com.extractor.unraveldocs.documents.utils.SanitizeLogging;
import com.extractor.unraveldocs.subscription.datamodel.SubscriptionPlans;
import com.extractor.unraveldocs.subscription.model.SubscriptionPlan;
import com.extractor.unraveldocs.subscription.model.UserSubscription;
import com.extractor.unraveldocs.subscription.repository.UserSubscriptionRepository;
import com.extractor.unraveldocs.user.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
@DisplayName("CouponValidationServiceImpl Tests")
class CouponValidationServiceImplTest {

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private CouponUsageRepository couponUsageRepository;

    @Mock
    private CouponRecipientRepository couponRecipientRepository;

    @Mock
    private UserSubscriptionRepository userSubscriptionRepository;

    @Mock
    private CouponMapper couponMapper;

    @Mock
    private SanitizeLogging sanitizer;

    @InjectMocks
    private CouponValidationServiceImpl validationService;

    private User testUser;
    private Coupon validCoupon;
    private UserSubscription activeSubscription;

    @BeforeEach
    void setUp() {
        // Configure sanitizer mock to return input value (lenient as it may not be called in all tests)
        lenient().when(sanitizer.sanitizeLogging(anyString())).thenAnswer(invocation -> invocation.getArgument(0));

        testUser = createTestUser();
        validCoupon = createValidCoupon();
        activeSubscription = createActiveSubscription();
    }

    // ========== Helper Methods ==========

    private User createTestUser() {
        User user = new User();
        user.setId("test-user-id");
        user.setEmail("user@example.com");
        user.setFirstName("Test");
        user.setLastName("User");
        user.setRole(Role.USER);
        user.setCreatedAt(OffsetDateTime.now().minusDays(60));
        return user;
    }

    private Coupon createValidCoupon() {
        Coupon coupon = new Coupon();
        coupon.setId("coupon-id-123");
        coupon.setCode("VALID20");
        coupon.setDiscountPercentage(new BigDecimal("20"));
        coupon.setValidFrom(OffsetDateTime.now().minusDays(1));
        coupon.setValidUntil(OffsetDateTime.now().plusDays(30));
        coupon.setMaxUsageCount(100);
        coupon.setCurrentUsageCount(10);
        coupon.setMaxUsagePerUser(1);
        coupon.setRecipientCategory(RecipientCategory.ALL_PAID_USERS);
        coupon.setActive(true);
        return coupon;
    }

    private UserSubscription createActiveSubscription() {
        SubscriptionPlan plan = new SubscriptionPlan();
        plan.setId("plan-id");
        plan.setName(SubscriptionPlans.PRO_MONTHLY);
        plan.setOcrPageLimit(1000);

        UserSubscription subscription = new UserSubscription();
        subscription.setId("sub-id");
        subscription.setUser(testUser);
        subscription.setPlan(plan);
        subscription.setStatus("ACTIVE");
        subscription.setCurrentPeriodEnd(OffsetDateTime.now().plusMonths(1));
        subscription.setOcrPagesUsed(500);
        return subscription;
    }

    // ========== Validate Coupon Tests ==========

    @Nested
    @DisplayName("Validate Coupon Tests")
    class ValidateCouponTests {

        @Test
        @DisplayName("Should validate active coupon successfully")
        void validateCoupon_success() {
            // Arrange
            when(couponRepository.findByCode("VALID20")).thenReturn(Optional.of(validCoupon));
            when(couponUsageRepository.countByCouponIdAndUserId(anyString(), anyString())).thenReturn(0);
            when(userSubscriptionRepository.findByUserId(testUser.getId()))
                    .thenReturn(Optional.of(activeSubscription));
            when(couponMapper.toCouponData(validCoupon)).thenReturn(mock(CouponData.class));

            // Act
            CouponValidationResponse result = validationService.validateCoupon("VALID20", testUser);

            // Assert
            assertNotNull(result);
            verify(couponRepository).findByCode("VALID20");
        }

        @Test
        @DisplayName("Should return invalid for non-existent coupon")
        void validateCoupon_failure_notFound() {
            // Arrange
            when(couponRepository.findByCode("INVALID")).thenReturn(Optional.empty());

            // Act
            CouponValidationResponse result = validationService.validateCoupon("INVALID", testUser);

            // Assert
            assertNotNull(result);
            assertFalse(result.isValid());
        }

        @Test
        @DisplayName("Should return invalid for inactive coupon")
        void validateCoupon_failure_inactive() {
            // Arrange
            validCoupon.setActive(false);
            when(couponRepository.findByCode("VALID20")).thenReturn(Optional.of(validCoupon));

            // Act
            CouponValidationResponse result = validationService.validateCoupon("VALID20", testUser);

            // Assert
            assertNotNull(result);
            assertFalse(result.isValid());
        }

        @Test
        @DisplayName("Should return invalid for expired coupon")
        void validateCoupon_failure_expired() {
            // Arrange
            validCoupon.setValidUntil(OffsetDateTime.now().minusDays(1));
            when(couponRepository.findByCode("VALID20")).thenReturn(Optional.of(validCoupon));

            // Act
            CouponValidationResponse result = validationService.validateCoupon("VALID20", testUser);

            // Assert
            assertNotNull(result);
            assertFalse(result.isValid());
        }

        @Test
        @DisplayName("Should return invalid for coupon not yet valid")
        void validateCoupon_failure_notYetValid() {
            // Arrange
            validCoupon.setValidFrom(OffsetDateTime.now().plusDays(7));
            when(couponRepository.findByCode("VALID20")).thenReturn(Optional.of(validCoupon));

            // Act
            CouponValidationResponse result = validationService.validateCoupon("VALID20", testUser);

            // Assert
            assertNotNull(result);
            assertFalse(result.isValid());
        }

        @Test
        @DisplayName("Should return invalid when usage limit reached")
        void validateCoupon_failure_usageLimitReached() {
            // Arrange
            validCoupon.setCurrentUsageCount(100); // equals maxUsageCount
            when(couponRepository.findByCode("VALID20")).thenReturn(Optional.of(validCoupon));

            // Act
            CouponValidationResponse result = validationService.validateCoupon("VALID20", testUser);

            // Assert
            assertNotNull(result);
            assertFalse(result.isValid());
        }
    }

    // ========== Apply Coupon Tests ==========

    @Nested
    @DisplayName("Apply Coupon Tests")
    class ApplyCouponTests {

        @Test
        @DisplayName("Should calculate discount correctly")
        void applyCoupon_success() {
            // Arrange
            ApplyCouponRequest request = ApplyCouponRequest.builder()
                    .couponCode("VALID20")
                    .amount(new BigDecimal("100.00"))
                    .build();

            when(couponRepository.findByCode("VALID20")).thenReturn(Optional.of(validCoupon));
            when(couponUsageRepository.countByCouponIdAndUserId(anyString(), anyString())).thenReturn(0);
            when(userSubscriptionRepository.findByUserId(testUser.getId()))
                    .thenReturn(Optional.of(activeSubscription));
            when(couponMapper.toCouponData(validCoupon)).thenReturn(mock(CouponData.class));

            // Act
            DiscountCalculationData result = validationService.applyCouponToAmount(request, testUser);

            // Assert
            assertNotNull(result);
        }

        @Test
        @DisplayName("Should return null when validation fails")
        void applyCoupon_failure_invalidCoupon() {
            // Arrange
            ApplyCouponRequest request = ApplyCouponRequest.builder()
                    .couponCode("INVALID")
                    .amount(new BigDecimal("100.00"))
                    .build();

            when(couponRepository.findByCode("INVALID")).thenReturn(Optional.empty());

            // Act
            DiscountCalculationData result = validationService.applyCouponToAmount(request, testUser);

            // Assert
            assertNull(result);
        }
    }
}
