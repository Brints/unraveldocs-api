package com.extractor.unraveldocs.credit.service;

import com.extractor.unraveldocs.coupon.dto.request.ApplyCouponRequest;
import com.extractor.unraveldocs.coupon.dto.response.DiscountCalculationData;
import com.extractor.unraveldocs.coupon.service.CouponValidationService;
import com.extractor.unraveldocs.credit.datamodel.CreditTransactionType;
import com.extractor.unraveldocs.credit.dto.request.PurchaseCreditPackRequest;
import com.extractor.unraveldocs.credit.dto.response.CreditPurchaseData;
import com.extractor.unraveldocs.credit.model.CreditPack;
import com.extractor.unraveldocs.credit.model.UserCreditBalance;
import com.extractor.unraveldocs.credit.repository.CreditPackRepository;
import com.extractor.unraveldocs.documents.utils.SanitizeLogging;
import com.extractor.unraveldocs.exceptions.custom.BadRequestException;
import com.extractor.unraveldocs.exceptions.custom.NotFoundException;
import com.extractor.unraveldocs.messaging.emailtemplates.UserEmailTemplateService;
import com.extractor.unraveldocs.payment.common.dto.PaymentRequest;
import com.extractor.unraveldocs.payment.common.dto.PaymentResponse;
import com.extractor.unraveldocs.payment.common.enums.PaymentGateway;
import com.extractor.unraveldocs.payment.common.service.PaymentGatewayFactory;
import com.extractor.unraveldocs.payment.common.service.PaymentGatewayService;
import com.extractor.unraveldocs.pushnotification.datamodel.NotificationType;
import com.extractor.unraveldocs.pushnotification.interfaces.NotificationService;
import com.extractor.unraveldocs.user.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Service for handling credit pack purchases.
 * Integrates with payment gateways, coupon validation, and notifications.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CreditPurchaseService {

    private final CreditPackRepository creditPackRepository;
    private final CreditBalanceService creditBalanceService;
    private final PaymentGatewayFactory paymentGatewayFactory;
    private final CouponValidationService couponValidationService;
    private final NotificationService notificationService;
    private final UserEmailTemplateService emailTemplateService;
    private final SanitizeLogging sanitizer;

    /**
     * Initialize a credit pack purchase.
     * Validates the pack, applies coupon if provided, and creates a one-time
     * payment.
     *
     * @param user    The purchasing user
     * @param request The purchase request
     * @return Purchase data with payment URL
     */
    public CreditPurchaseData initializePurchase(User user, PurchaseCreditPackRequest request) {
        // Validate credit pack
        CreditPack pack = creditPackRepository.findById(request.getCreditPackId())
                .orElseThrow(() -> new NotFoundException("Credit pack not found: " + request.getCreditPackId()));

        if (!pack.getIsActive()) {
            throw new BadRequestException("This credit pack is no longer available");
        }

        // Parse gateway
        PaymentGateway gateway;
        try {
            gateway = PaymentGateway.valueOf(request.getGateway().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Unsupported payment gateway: " + request.getGateway());
        }

        // Calculate amount (with optional coupon discount)
        long originalAmountInCents = pack.getPriceInCents();
        long finalAmountInCents = originalAmountInCents;
        long discountApplied = 0;

        if (request.getCouponCode() != null && !request.getCouponCode().isBlank()) {
            ApplyCouponRequest couponRequest = ApplyCouponRequest.builder()
                    .couponCode(request.getCouponCode())
                    .amount(BigDecimal.valueOf(originalAmountInCents))
                    .subscriptionPlan(pack.getName().name())
                    .build();

            DiscountCalculationData discountData = couponValidationService.applyCouponToAmount(couponRequest, user);
            finalAmountInCents = discountData.getFinalAmount().longValue();
            discountApplied = originalAmountInCents - finalAmountInCents;
        }

        // Create one-time payment via payment gateway
        PaymentGatewayService gatewayService = paymentGatewayFactory.getProvider(gateway);

        PaymentRequest paymentRequest = PaymentRequest.builder()
                .amount(finalAmountInCents)
                .currency(pack.getCurrency())
                .description("Credit Pack: " + pack.getDisplayName() + " (" + pack.getCreditsIncluded() + " credits)")
                .receiptEmail(user.getEmail())
                .metadata(Map.of(
                        "type", "CREDIT_PURCHASE",
                        "creditPackId", pack.getId(),
                        "creditsIncluded", String.valueOf(pack.getCreditsIncluded()),
                        "userId", user.getId()))
                .build();

        PaymentResponse paymentResponse = gatewayService.createPayment(user, paymentRequest);

        if (!paymentResponse.isSuccess()) {
            throw new BadRequestException("Payment initialization failed: " + paymentResponse.getErrorMessage());
        }

        return CreditPurchaseData.builder()
                .paymentUrl(paymentResponse.getPaymentUrl())
                .reference(paymentResponse.getProviderPaymentId())
                .packName(pack.getDisplayName())
                .creditsToReceive(pack.getCreditsIncluded())
                .amountInCents(finalAmountInCents)
                .discountApplied(discountApplied)
                .build();
    }

    /**
     * Complete a credit pack purchase after payment confirmation.
     * Adds credits to user balance and sends notifications (push + email).
     *
     * @param user             The purchasing user
     * @param creditPackId     The purchased credit pack ID
     * @param paymentReference The payment reference for auditing
     */
    @Transactional
    public void completePurchase(User user, String creditPackId, String paymentReference) {
        CreditPack pack = creditPackRepository.findById(creditPackId)
                .orElseThrow(() -> new NotFoundException("Credit pack not found: " + creditPackId));

        // Add credits to user balance
        creditBalanceService.addCredits(
                user,
                pack.getCreditsIncluded(),
                CreditTransactionType.PURCHASE,
                paymentReference,
                "Purchased " + pack.getDisplayName() + " (" + pack.getCreditsIncluded() + " credits)");

        UserCreditBalance balance = creditBalanceService.getOrCreateBalance(user.getId());

        // Send push notification
        try {
            notificationService.sendToUser(
                    user.getId(),
                    NotificationType.CREDIT_PURCHASE_SUCCESS,
                    "Credit Purchase Successful",
                    String.format("%d credits added to your account from %s. Current balance: %d",
                            pack.getCreditsIncluded(), pack.getDisplayName(), balance.getBalance()),
                    Map.of("packName", pack.getDisplayName(),
                            "creditsAdded", String.valueOf(pack.getCreditsIncluded())));
        } catch (Exception e) {
            log.error("Failed to send credit purchase push notification for user {}: {}", sanitizer.sanitizeLogging(user.getId()), e.getMessage());
        }

        // Send email notification
        try {
            emailTemplateService.sendCreditPurchaseEmail(
                    user.getEmail(),
                    user.getFirstName(),
                    user.getLastName(),
                    pack.getDisplayName(),
                    pack.getCreditsIncluded(),
                    balance.getBalance());
        } catch (Exception e) {
            log.error("Failed to send credit purchase email for user {}: {}", sanitizer.sanitizeLogging(user.getId()), e.getMessage());
        }
    }
}
