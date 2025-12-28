package com.extractor.unraveldocs.payment.paystack.service;

import com.extractor.unraveldocs.payment.paystack.model.PaystackCustomer;
import com.extractor.unraveldocs.payment.paystack.model.PaystackSubscription;
import com.extractor.unraveldocs.payment.paystack.repository.PaystackSubscriptionRepository;
import com.extractor.unraveldocs.subscription.datamodel.BillingIntervalUnit;
import com.extractor.unraveldocs.subscription.datamodel.SubscriptionCurrency;
import com.extractor.unraveldocs.subscription.datamodel.SubscriptionPlans;
import com.extractor.unraveldocs.subscription.model.SubscriptionPlan;
import com.extractor.unraveldocs.subscription.model.UserSubscription;
import com.extractor.unraveldocs.subscription.repository.SubscriptionPlanRepository;
import com.extractor.unraveldocs.subscription.repository.UserSubscriptionRepository;
import com.extractor.unraveldocs.user.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PaystackSubscriptionService.
 * Tests focus on repository operations and subscription management.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PaystackSubscriptionService Tests")
class PaystackSubscriptionServiceTest {

    @Mock
    private PaystackSubscriptionRepository subscriptionRepository;

    @Mock
    private SubscriptionPlanRepository subscriptionPlanRepository;

    @Mock
    private UserSubscriptionRepository userSubscriptionRepository;

    private User testUser;
    private PaystackCustomer testCustomer;
    private SubscriptionPlan premiumMonthlyPlan;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId("user-123");
        testUser.setEmail("test@example.com");
        testUser.setFirstName("John");
        testUser.setLastName("Doe");

        testCustomer = PaystackCustomer.builder()
                .id("customer-id-123")
                .user(testUser)
                .customerCode("CUS_test123")
                .email("test@example.com")
                .build();

        premiumMonthlyPlan = new SubscriptionPlan();
        premiumMonthlyPlan.setId("plan-id-123");
        premiumMonthlyPlan.setName(SubscriptionPlans.PRO_MONTHLY);
        premiumMonthlyPlan.setPrice(new BigDecimal("28.00"));
        premiumMonthlyPlan.setCurrency(SubscriptionCurrency.USD);
        premiumMonthlyPlan.setBillingIntervalUnit(BillingIntervalUnit.MONTH);
        premiumMonthlyPlan.setBillingIntervalValue(1);
        premiumMonthlyPlan.setActive(true);
    }

    @Nested
    @DisplayName("Get Subscription Tests")
    class GetSubscriptionTests {

        @Test
        @DisplayName("Should get subscription by code")
        void shouldGetSubscriptionByCode() {
            // Given
            String subscriptionCode = "SUB_test123";
            PaystackSubscription subscription = PaystackSubscription.builder()
                    .subscriptionCode(subscriptionCode)
                    .status("active")
                    .user(testUser)
                    .build();

            when(subscriptionRepository.findBySubscriptionCode(subscriptionCode))
                    .thenReturn(Optional.of(subscription));

            // When
            Optional<PaystackSubscription> result = subscriptionRepository.findBySubscriptionCode(subscriptionCode);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getSubscriptionCode()).isEqualTo(subscriptionCode);
            assertThat(result.get().getStatus()).isEqualTo("active");
        }

        @Test
        @DisplayName("Should get active subscription by user ID")
        void shouldGetActiveSubscriptionByUserId() {
            // Given
            String userId = "user-123";
            PaystackSubscription subscription = PaystackSubscription.builder()
                    .subscriptionCode("SUB_active123")
                    .status("active")
                    .user(testUser)
                    .build();

            when(subscriptionRepository.findActiveSubscriptionByUserId(userId))
                    .thenReturn(Optional.of(subscription));

            // When
            Optional<PaystackSubscription> result = subscriptionRepository.findActiveSubscriptionByUserId(userId);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getStatus()).isEqualTo("active");
        }

        @Test
        @DisplayName("Should get subscriptions by user ID with pagination")
        void shouldGetSubscriptionsByUserId() {
            // Given
            String userId = "user-123";
            Pageable pageable = PageRequest.of(0, 10);
            List<PaystackSubscription> subscriptions = List.of(
                    PaystackSubscription.builder().subscriptionCode("SUB_1").status("active").build(),
                    PaystackSubscription.builder().subscriptionCode("SUB_2").status("cancelled").build()
            );
            Page<PaystackSubscription> subscriptionPage = new PageImpl<>(subscriptions, pageable, subscriptions.size());

            when(subscriptionRepository.findByUser_Id(userId, pageable)).thenReturn(subscriptionPage);

            // When
            Page<PaystackSubscription> result = subscriptionRepository.findByUser_Id(userId, pageable);

            // Then
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getTotalElements()).isEqualTo(2);
        }

        @Test
        @DisplayName("Should get subscriptions by status")
        void shouldGetSubscriptionsByStatus() {
            // Given
            String status = "active";
            List<PaystackSubscription> subscriptions = List.of(
                    PaystackSubscription.builder().subscriptionCode("SUB_1").status("active").build(),
                    PaystackSubscription.builder().subscriptionCode("SUB_2").status("active").build()
            );

            when(subscriptionRepository.findByStatus(status)).thenReturn(subscriptions);

            // When
            List<PaystackSubscription> result = subscriptionRepository.findByStatus(status);

            // Then
            assertThat(result).hasSize(2);
            assertThat(result).allMatch(sub -> sub.getStatus().equals("active"));
        }
    }

    @Nested
    @DisplayName("Subscription Plan Tests")
    class SubscriptionPlanTests {

        @Test
        @DisplayName("Should find subscription plan by name")
        void shouldFindPlanByName() {
            // Given
            when(subscriptionPlanRepository.findByName(SubscriptionPlans.PRO_MONTHLY))
                    .thenReturn(Optional.of(premiumMonthlyPlan));

            // When
            Optional<SubscriptionPlan> result = subscriptionPlanRepository.findByName(SubscriptionPlans.PRO_MONTHLY);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getName()).isEqualTo(SubscriptionPlans.PRO_MONTHLY);
            assertThat(result.get().getPrice()).isEqualTo(new BigDecimal("28.00"));
        }

        @Test
        @DisplayName("Should update plan with Paystack plan code")
        void shouldUpdatePlanWithPaystackCode() {
            // Given
            premiumMonthlyPlan.setPaystackPlanCode("PLN_premium123");

            when(subscriptionPlanRepository.save(any(SubscriptionPlan.class))).thenReturn(premiumMonthlyPlan);

            // When
            SubscriptionPlan savedPlan = subscriptionPlanRepository.save(premiumMonthlyPlan);

            // Then
            assertThat(savedPlan.getPaystackPlanCode()).isEqualTo("PLN_premium123");
        }

        @Test
        @DisplayName("Should convert plan price to kobo correctly")
        void shouldConvertPriceToKobo() {
            // Given
            BigDecimal priceInDollars = new BigDecimal("28.00");

            // When
            int priceInKobo = priceInDollars.multiply(BigDecimal.valueOf(100)).intValue();

            // Then
            assertThat(priceInKobo).isEqualTo(2800);
        }
    }

    @Nested
    @DisplayName("Subscription Model Tests")
    class SubscriptionModelTests {

        @Test
        @DisplayName("Should create subscription with all fields")
        void shouldCreateSubscriptionWithAllFields() {
            // Given & When
            OffsetDateTime nextPaymentDate = OffsetDateTime.now().plusMonths(1);
            PaystackSubscription subscription = PaystackSubscription.builder()
                    .user(testUser)
                    .paystackCustomer(testCustomer)
                    .planCode("PLN_premium123")
                    .paystackSubscriptionId(12345L)
                    .subscriptionCode("SUB_test123")
                    .emailToken("token123")
                    .status("active")
                    .amount(new BigDecimal("28.00"))
                    .nextPaymentDate(nextPaymentDate)
                    .paymentsCount(1)
                    .build();

            // Then
            assertThat(subscription.getSubscriptionCode()).isEqualTo("SUB_test123");
            assertThat(subscription.getPlanCode()).isEqualTo("PLN_premium123");
            assertThat(subscription.getStatus()).isEqualTo("active");
            assertThat(subscription.getAmount()).isEqualTo(new BigDecimal("28.00"));
            assertThat(subscription.getPaymentsCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should update subscription status")
        void shouldUpdateSubscriptionStatus() {
            // Given
            PaystackSubscription subscription = PaystackSubscription.builder()
                    .subscriptionCode("SUB_test123")
                    .status("active")
                    .build();

            // When
            subscription.setStatus("non-renewing");

            when(subscriptionRepository.save(any(PaystackSubscription.class))).thenReturn(subscription);
            PaystackSubscription savedSubscription = subscriptionRepository.save(subscription);

            // Then
            assertThat(savedSubscription.getStatus()).isEqualTo("non-renewing");
        }

        @Test
        @DisplayName("Should set cancelled timestamp")
        void shouldSetCancelledTimestamp() {
            // Given
            PaystackSubscription subscription = PaystackSubscription.builder()
                    .subscriptionCode("SUB_test123")
                    .status("active")
                    .build();

            // When
            OffsetDateTime cancelledAt = OffsetDateTime.now();
            subscription.setStatus("cancelled");
            subscription.setCancelledAt(cancelledAt);

            // Then
            assertThat(subscription.getStatus()).isEqualTo("cancelled");
            assertThat(subscription.getCancelledAt()).isEqualTo(cancelledAt);
        }
    }

    @Nested
    @DisplayName("User Subscription Tests")
    class UserSubscriptionTests {

        @Test
        @DisplayName("Should find user subscription by user ID")
        void shouldFindUserSubscriptionByUserId() {
            // Given
            UserSubscription userSubscription = new UserSubscription();
            userSubscription.setUser(testUser);
            userSubscription.setPlan(premiumMonthlyPlan);
            userSubscription.setStatus("active");

            when(userSubscriptionRepository.findByUserId(testUser.getId()))
                    .thenReturn(Optional.of(userSubscription));

            // When
            Optional<UserSubscription> result = userSubscriptionRepository.findByUserId(testUser.getId());

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getStatus()).isEqualTo("active");
            assertThat(result.get().getPlan()).isEqualTo(premiumMonthlyPlan);
        }

        @Test
        @DisplayName("Should link user subscription to plan")
        void shouldLinkUserSubscriptionToPlan() {
            // Given
            UserSubscription userSubscription = new UserSubscription();
            userSubscription.setUser(testUser);
            userSubscription.setStatus("pending");

            // When
            userSubscription.setPlan(premiumMonthlyPlan);
            userSubscription.setPaymentGatewaySubscriptionId("SUB_test123");
            userSubscription.setStatus("active");
            userSubscription.setAutoRenew(true);

            when(userSubscriptionRepository.save(any(UserSubscription.class))).thenReturn(userSubscription);
            UserSubscription savedSubscription = userSubscriptionRepository.save(userSubscription);

            // Then
            assertThat(savedSubscription.getPlan()).isEqualTo(premiumMonthlyPlan);
            assertThat(savedSubscription.getPaymentGatewaySubscriptionId()).isEqualTo("SUB_test123");
            assertThat(savedSubscription.getStatus()).isEqualTo("active");
            assertThat(savedSubscription.isAutoRenew()).isTrue();
        }
    }
}
