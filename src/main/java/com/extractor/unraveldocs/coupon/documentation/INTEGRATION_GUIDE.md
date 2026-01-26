# Coupon System Integration Guide

This guide explains how to integrate the coupon system with the payment and subscription flows.

---

## Table of Contents

1. [Payment Integration](#payment-integration)
2. [Subscription Flow](#subscription-flow)
3. [Webhook Handling](#webhook-handling)

---

## Payment Integration

### Applying Coupon During Checkout

When a user applies a coupon during checkout, follow these steps:

```java
// 1. Validate the coupon
@Autowired
private CouponValidationService couponValidationService;

public PaymentResult processPayment(PaymentRequest request, User user) {
    BigDecimal finalAmount = request.getAmount();
    Coupon appliedCoupon = null;
    
    // Check if coupon code provided
    if (request.getCouponCode() != null) {
        // 2. Validate coupon
        CouponValidationResponse validation = couponValidationService
            .validateCoupon(request.getCouponCode(), user);
        
        if (!validation.isValid()) {
            throw new InvalidCouponException(validation.getErrorMessage());
        }
        
        // 3. Calculate discount
        ApplyCouponRequest couponRequest = ApplyCouponRequest.builder()
            .couponCode(request.getCouponCode())
            .amount(request.getAmount())
            .build();
            
        DiscountCalculationData discount = couponValidationService
            .applyCouponToAmount(couponRequest, user);
        
        // Fetch the payment currency
        String currency = request.getCurrency();
        
        if (!discount.isMinPurchaseRequirementMet()) {
            throw new MinPurchaseNotMetException(
                "Minimum purchase of " + currency + discount.getMinPurchaseAmount() + " required"
            );
        }
        
        finalAmount = discount.getFinalAmount();
        appliedCoupon = getCouponByCode(request.getCouponCode());
    }
    
    // 4. Process payment with discounted amount
    PaymentResult result = paymentProcessor.charge(finalAmount);
    
    // 5. Record coupon usage after successful payment
    if (result.isSuccessful() && appliedCoupon != null) {
        couponValidationService.recordCouponUsage(
            appliedCoupon,
            user,
            request.getAmount(),
            finalAmount,
            result.getPaymentReference(),
            request.getSubscriptionPlan()
        );
    }
    
    return result;
}
```

### Paystack Integration

```java
@Service
public class PaystackPaymentService {
    
    @Autowired
    private CouponValidationService couponValidationService;
    
    public PaystackChargeResponse initiatePayment(
            User user, 
            SubscriptionPlan plan, 
            String couponCode) {
        
        BigDecimal amount = plan.getPrice();
        
        // Apply coupon if provided
        if (couponCode != null) {
            DiscountCalculationData discount = couponValidationService
                .applyCouponToAmount(
                    ApplyCouponRequest.builder()
                        .couponCode(couponCode)
                        .amount(amount)
                        .build(),
                    user
                );
            
            if (discount != null && discount.isMinPurchaseRequirementMet()) {
                amount = discount.getFinalAmount();
            }
        }
        
        // Initialize Paystack transaction with discounted amount
        return paystackClient.initializeTransaction(
            user.getEmail(),
            amount.multiply(BigDecimal.valueOf(100)).intValue() // kobo
        );
    }
}
```

---

## Subscription Flow

### Subscription Creation with Coupon

```java
@Service
public class SubscriptionService {
    
    @Autowired
    private CouponValidationService couponValidationService;
    
    @Transactional
    public UserSubscription createSubscription(
            User user, 
            SubscriptionPlan plan, 
            String couponCode,
            String paymentReference) {
        
        BigDecimal originalPrice = plan.getPrice();
        BigDecimal finalPrice = originalPrice;
        String appliedCouponCode = null;
        
        // Validate and apply coupon
        if (couponCode != null && !couponCode.isBlank()) {
            CouponValidationResponse validation = couponValidationService
                .validateCoupon(couponCode, user);
            
            if (validation.isValid()) {
                ApplyCouponRequest couponRequest = ApplyCouponRequest.builder()
                    .couponCode(couponCode)
                    .amount(originalPrice)
                    .build();
                
                DiscountCalculationData discount = couponValidationService
                    .applyCouponToAmount(couponRequest, user);
                
                if (discount.isMinPurchaseRequirementMet()) {
                    finalPrice = discount.getFinalAmount();
                    appliedCouponCode = couponCode;
                    
                    // Record the usage
                    Coupon coupon = couponRepository.findByCode(couponCode)
                        .orElseThrow();
                    
                    couponValidationService.recordCouponUsage(
                        coupon,
                        user,
                        originalPrice,
                        finalPrice,
                        paymentReference,
                        plan.getName().toString()
                    );
                }
            }
        }
        
        // Create subscription with final price
        UserSubscription subscription = UserSubscription.builder()
            .user(user)
            .plan(plan)
            .status(SubscriptionStatus.ACTIVE)
            .paidAmount(finalPrice)
            .appliedCouponCode(appliedCouponCode)
            .discountAmount(originalPrice.subtract(finalPrice))
            .build();
        
        return subscriptionRepository.save(subscription);
    }
}
```
---

## Webhook Handling

### Handling Payment Webhooks with Coupons

```java
@RestController
@RequestMapping("/webhooks")
public class PaymentWebhookController {
    
    @Autowired
    private CouponValidationService couponValidationService;
    
    @Autowired
    private SubscriptionService subscriptionService;
    
    @PostMapping("/paystack")
    public ResponseEntity<?> handlePaystackWebhook(
            @RequestBody PaystackWebhookPayload payload,
            @RequestHeader("x-paystack-signature") String signature) {
        
        // Verify signature
        if (!verifySignature(payload, signature)) {
            return ResponseEntity.status(401).build();
        }
        
        if ("charge.success".equals(payload.getEvent())) {
            PaystackChargeData data = payload.getData();
            
            // Extract coupon from metadata
            String couponCode = data.getMetadata().getCouponCode();
            User user = userRepository.findByEmail(data.getCustomer().getEmail())
                .orElseThrow();
            
            // Record coupon usage
            if (couponCode != null) {
                Coupon coupon = couponRepository.findByCode(couponCode)
                    .orElse(null);
                
                if (coupon != null) {
                    BigDecimal originalAmount = data.getMetadata().getOriginalAmount();
                    BigDecimal paidAmount = BigDecimal.valueOf(data.getAmount())
                        .divide(BigDecimal.valueOf(100));
                    
                    couponValidationService.recordCouponUsage(
                        coupon,
                        user,
                        originalAmount,
                        paidAmount,
                        data.getReference(),
                        data.getMetadata().getPlan()
                    );
                }
            }
            
            // Process subscription...
        }
        
        return ResponseEntity.ok().build();
    }
}
```

---

### End-to-End Test Flow

```gherkin
Feature: Coupon Application in Checkout
  
  Scenario: User applies valid coupon during subscription purchase
    Given I am logged in as a user
    And a coupon "SAVE20" exists with 20% discount
    And I am on the checkout page for "PRO_MONTHLY" plan at $99.00
    
    When I enter coupon code "SAVE20"
    And I click "Apply"
    
    Then I should see "Coupon applied: 20% off"
    And I should see the discounted price "$79.20" or "NGN 31,680.00" depending on currency
    
    When I complete the payment
    
    Then my subscription should be created with plan "PRO_MONTHLY"
    And the coupon usage count should increase by 1
    And I should receive a confirmation email
```

---

## Best Practices

1. **Always validate before payment** - Never trust client-side calculations
2. **Record usage after payment success** - Prevents counting failed payments
3. **Handle race conditions** - Use database locks for usage limits
4. **Cache wisely** - Invalidate on updates, warm cache on startup
5. **Log all operations** - Audit trail for disputes

---
