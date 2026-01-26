package com.extractor.unraveldocs.coupon.service.impl;

import com.extractor.unraveldocs.auth.datamodel.Role;
import com.extractor.unraveldocs.coupon.dto.CouponData;
import com.extractor.unraveldocs.coupon.dto.request.CreateCouponRequest;
import com.extractor.unraveldocs.coupon.enums.RecipientCategory;
import com.extractor.unraveldocs.coupon.helpers.CouponCodeGenerator;
import com.extractor.unraveldocs.coupon.helpers.CouponMapper;
import com.extractor.unraveldocs.coupon.model.Coupon;
import com.extractor.unraveldocs.coupon.repository.CouponAnalyticsRepository;
import com.extractor.unraveldocs.coupon.repository.CouponRecipientRepository;
import com.extractor.unraveldocs.coupon.repository.CouponRepository;
import com.extractor.unraveldocs.coupon.repository.CouponTemplateRepository;
import com.extractor.unraveldocs.coupon.repository.CouponUsageRepository;
import com.extractor.unraveldocs.coupon.service.CouponCacheService;
import com.extractor.unraveldocs.coupon.service.CouponNotificationService;
import com.extractor.unraveldocs.documents.utils.SanitizeLogging;
import com.extractor.unraveldocs.shared.response.ResponseBuilderService;
import com.extractor.unraveldocs.user.model.User;
import com.extractor.unraveldocs.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CouponServiceImpl Tests")
class CouponServiceImplTest {

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private CouponUsageRepository couponUsageRepository;

    @Mock
    private CouponRecipientRepository couponRecipientRepository;

    @Mock
    private CouponTemplateRepository couponTemplateRepository;

    @Mock
    private CouponAnalyticsRepository couponAnalyticsRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CouponCodeGenerator codeGenerator;

    @Mock
    private CouponMapper couponMapper;

    @Mock
    private CouponCacheService cacheService;

    @Mock
    private CouponNotificationService notificationService;

    @Mock
    private ResponseBuilderService responseBuilderService;

    @Mock
    private SanitizeLogging sanitizer;

    @InjectMocks
    private CouponServiceImpl couponService;

    private User adminUser;
    private Coupon testCoupon;
    private CreateCouponRequest createRequest;
    private CouponData couponData;

    @BeforeEach
    void setUp() {
        // Configure sanitizer mock to return input value (lenient as it may not be called in all tests)
        lenient().when(sanitizer.sanitizeLogging(anyString())).thenAnswer(invocation -> invocation.getArgument(0));

        adminUser = createAdminUser();
        testCoupon = createTestCoupon();
        createRequest = createTestRequest();
        couponData = createCouponData();
    }

    private User createAdminUser() {
        User user = new User();
        user.setId("admin-user-id");
        user.setEmail("admin@example.com");
        user.setFirstName("Admin");
        user.setLastName("User");
        user.setRole(Role.ADMIN);
        return user;
    }

    private Coupon createTestCoupon() {
        Coupon coupon = new Coupon();
        coupon.setId("coupon-id-123");
        coupon.setCode("SAVE20OFF");
        coupon.setDescription("20% off subscription");
        coupon.setDiscountPercentage(new BigDecimal("20"));
        coupon.setValidFrom(OffsetDateTime.now());
        coupon.setValidUntil(OffsetDateTime.now().plusMonths(1));
        coupon.setMaxUsageCount(100);
        coupon.setCurrentUsageCount(0);
        coupon.setMaxUsagePerUser(1);
        coupon.setRecipientCategory(RecipientCategory.ALL_PAID_USERS);
        coupon.setActive(true);
        coupon.setCreatedBy(adminUser);
        return coupon;
    }

    private CreateCouponRequest createTestRequest() {
        return CreateCouponRequest.builder()
                .description("20% off subscription")
                .discountPercentage(new BigDecimal("20"))
                .validFrom(OffsetDateTime.now())
                .validUntil(OffsetDateTime.now().plusMonths(1))
                .maxUsageCount(100)
                .maxUsagePerUser(1)
                .recipientCategory(RecipientCategory.ALL_PAID_USERS)
                .build();
    }

    private CouponData createCouponData() {
        return CouponData.builder()
                .id("coupon-id-123")
                .code("SAVE20OFF")
                .description("20% off subscription")
                .discountPercentage(new BigDecimal("20"))
                .validFrom(OffsetDateTime.now().toString())
                .validUntil(OffsetDateTime.now().plusMonths(1).toString())
                .isActive(true)
                .build();
    }

    // ========== Create Coupon Tests ==========

    @Nested
    @DisplayName("Create Coupon Tests")
    class CreateCouponTests {

        @Test
        @DisplayName("Should save coupon to repository when created")
        void createCoupon_savesCoupon() {
            // Arrange
            when(codeGenerator.generate(any())).thenReturn("GENERATED");
            when(couponRepository.existsByCode(anyString())).thenReturn(false);
            when(couponRepository.save(any(Coupon.class))).thenReturn(testCoupon);
            when(couponMapper.toCouponData(any(Coupon.class))).thenReturn(couponData);
            when(responseBuilderService.buildUserResponse(any(), eq(HttpStatus.CREATED), anyString()))
                    .thenReturn(null);

            // Act
            couponService.createCoupon(createRequest, adminUser);

            // Assert
            verify(couponRepository).save(any(Coupon.class));
        }

        @Test
        @DisplayName("Should not save when duplicate code exists")
        void createCoupon_duplicateCode_doesNotSave() {
            // Arrange
            CreateCouponRequest requestWithCustomCode = CreateCouponRequest.builder()
                    .customCode("EXISTING")
                    .description("Duplicate code")
                    .discountPercentage(new BigDecimal("10"))
                    .validFrom(OffsetDateTime.now())
                    .validUntil(OffsetDateTime.now().plusMonths(1))
                    .maxUsageCount(10)
                    .maxUsagePerUser(1)
                    .recipientCategory(RecipientCategory.ALL_PAID_USERS)
                    .build();

            when(couponRepository.existsByCode("EXISTING")).thenReturn(true);
            when(responseBuilderService.buildUserResponse(isNull(), eq(HttpStatus.CONFLICT), anyString()))
                    .thenReturn(null);

            // Act
            couponService.createCoupon(requestWithCustomCode, adminUser);

            // Assert
            verify(couponRepository, never()).save(any(Coupon.class));
        }
    }

    // ========== Get Coupon Tests ==========

    @Nested
    @DisplayName("Get Coupon Tests")
    class GetCouponTests {

        @Test
        @DisplayName("Should return coupon by ID")
        void getCouponById_success() {
            // Arrange
            when(couponRepository.findById("coupon-id-123")).thenReturn(Optional.of(testCoupon));
            when(couponMapper.toCouponData(testCoupon)).thenReturn(couponData);
            when(responseBuilderService.buildUserResponse(any(), eq(HttpStatus.OK), anyString()))
                    .thenReturn(null);

            // Act
            couponService.getCouponById("coupon-id-123");

            // Assert
            verify(couponRepository).findById("coupon-id-123");
        }

        @Test
        @DisplayName("Should check cache first for coupon by code")
        void getCouponByCode_checksCacheFirst() {
            // Arrange
            when(cacheService.getCouponByCode("SAVE20OFF")).thenReturn(Optional.of(couponData));
            when(responseBuilderService.buildUserResponse(any(), eq(HttpStatus.OK), anyString()))
                    .thenReturn(null);

            // Act
            couponService.getCouponByCode("SAVE20OFF");

            // Assert
            verify(cacheService).getCouponByCode("SAVE20OFF");
        }

        @Test
        @DisplayName("Should fall back to database when cache miss")
        void getCouponByCode_fallbackToDatabase() {
            // Arrange
            when(cacheService.getCouponByCode("SAVE20OFF")).thenReturn(Optional.empty());
            when(couponRepository.findByCode("SAVE20OFF")).thenReturn(Optional.of(testCoupon));
            when(couponMapper.toCouponData(testCoupon)).thenReturn(couponData);
            when(responseBuilderService.buildUserResponse(any(), eq(HttpStatus.OK), anyString()))
                    .thenReturn(null);

            // Act
            couponService.getCouponByCode("SAVE20OFF");

            // Assert
            verify(couponRepository).findByCode("SAVE20OFF");
        }
    }
}
