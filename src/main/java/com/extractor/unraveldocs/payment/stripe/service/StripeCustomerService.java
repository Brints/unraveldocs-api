package com.extractor.unraveldocs.payment.stripe.service;

import com.extractor.unraveldocs.documents.utils.SanitizeLogging;
import com.extractor.unraveldocs.payment.stripe.dto.response.CustomerResponse;
import com.extractor.unraveldocs.payment.stripe.exception.CustomerNotFoundException;
import com.extractor.unraveldocs.payment.stripe.exception.StripePaymentException;
import com.extractor.unraveldocs.payment.stripe.model.StripeCustomer;
import com.extractor.unraveldocs.payment.stripe.repository.StripeCustomerRepository;
import com.extractor.unraveldocs.user.model.User;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.PaymentMethod;
import com.stripe.model.PaymentMethodCollection;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.CustomerListPaymentMethodsParams;
import com.stripe.param.CustomerUpdateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for managing Stripe customers
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StripeCustomerService {
    private final SanitizeLogging sanitize;

    private final StripeCustomerRepository stripeCustomerRepository;

    /**
     * Create or retrieve existing Stripe customer for a user
     */
    @Transactional
    public StripeCustomer getOrCreateCustomer(User user) throws StripeException {
        return stripeCustomerRepository.findByUser(user)
                .orElseGet(() -> createNewCustomer(user));
    }

    /**
     * Create a new Stripe customer
     */
    @Transactional
    public StripeCustomer createNewCustomer(User user) {
        try {
            // Create customer in Stripe
            Map<String, String> metadata = new HashMap<>();
            metadata.put("user_id", user.getId());
            metadata.put("email", user.getEmail());

            CustomerCreateParams params = CustomerCreateParams.builder()
                    .setEmail(user.getEmail())
                    .setName(user.getFirstName() + " " + user.getLastName())
                    .putAllMetadata(metadata)
                    .build();

            Customer stripeCustomer = Customer.create(params);

            // Save customer mapping to database
            StripeCustomer customer = new StripeCustomer();
            customer.setUser(user);
            customer.setStripeCustomerId(stripeCustomer.getId());
            customer.setEmail(user.getEmail());
            customer.setCustomerName(user.getFirstName() + " " + user.getLastName());
            customer.setActive(true);

            StripeCustomer savedCustomer = stripeCustomerRepository.save(customer);
            log.info("Created Stripe customer {} for user {}", sanitize.sanitizeLogging(stripeCustomer.getId()), sanitize.sanitizeLogging(user.getId()));

            return savedCustomer;
        } catch (StripeException e) {
            log.error("Failed to create Stripe customer for user {}: {}", sanitize.sanitizeLogging(user.getId()), e.getMessage());
            throw new StripePaymentException("Failed to create customer", e);
        }
    }

    /**
     * Get customer by user ID
     */
    public StripeCustomer getCustomerByUserId(String userId) {
        return stripeCustomerRepository.findByUserId(userId)
                .orElseThrow(() -> CustomerNotFoundException.forUser(userId));
    }

    /**
     * Update customer information
     */
    @Transactional
    public StripeCustomer updateCustomer(String userId, String name, String email) throws StripeException {
        StripeCustomer customer = getCustomerByUserId(userId);

        try {
            CustomerUpdateParams params = CustomerUpdateParams.builder()
                    .setName(name)
                    .setEmail(email)
                    .build();

            Customer.retrieve(customer.getStripeCustomerId()).update(params);

            customer.setCustomerName(name);
            customer.setEmail(email);

            return stripeCustomerRepository.save(customer);
        } catch (StripeException e) {
            log.error("Failed to update Stripe customer {}: {}", sanitize.sanitizeLogging(customer.getStripeCustomerId()), e.getMessage());
            throw e;
        }
    }

    /**
     * Attach payment method to customer
     */
    public void attachPaymentMethod(String userId, String paymentMethodId) throws StripeException {
        StripeCustomer customer = getCustomerByUserId(userId);

        try {
            PaymentMethod paymentMethod = PaymentMethod.retrieve(paymentMethodId);

            Map<String, Object> params = new HashMap<>();
            params.put("customer", customer.getStripeCustomerId());

            paymentMethod.attach(params);

            log.info("Attached payment method {} to customer {}", sanitize.sanitizeLogging(paymentMethodId), sanitize.sanitizeLogging(customer.getStripeCustomerId()));
        } catch (StripeException e) {
            log.error("Failed to attach payment method {} to customer {}: {}",
                    sanitize.sanitizeLogging(paymentMethodId), sanitize.sanitizeLogging(customer.getStripeCustomerId()), e.getMessage());
            throw e;
        }
    }

    /**
     * Set default payment method for customer
     */
    @Transactional
    public void setDefaultPaymentMethod(String userId, String paymentMethodId) throws StripeException {
        StripeCustomer customer = getCustomerByUserId(userId);

        try {
            CustomerUpdateParams params = CustomerUpdateParams.builder()
                    .setInvoiceSettings(
                            CustomerUpdateParams.InvoiceSettings.builder()
                                    .setDefaultPaymentMethod(paymentMethodId)
                                    .build()
                    )
                    .build();

            Customer.retrieve(customer.getStripeCustomerId()).update(params);

            customer.setDefaultPaymentMethodId(paymentMethodId);
            stripeCustomerRepository.save(customer);

            log.info("Set default payment method {} for customer {}", paymentMethodId, sanitize.sanitizeLogging(customer.getStripeCustomerId()));
        } catch (StripeException e) {
            log.error("Failed to set default payment method for customer {}: {}",
                    sanitize.sanitizeLogging(customer.getStripeCustomerId()), e.getMessage());
            throw e;
        }
    }

    /**
     * Get customer details with payment methods
     */
    public CustomerResponse getCustomerDetails(String userId) throws StripeException {
        StripeCustomer customer = getCustomerByUserId(userId);

        try {
            Customer stripeCustomer = Customer.retrieve(customer.getStripeCustomerId());

            // Get payment methods
            CustomerListPaymentMethodsParams params = CustomerListPaymentMethodsParams.builder()
                    .setType(CustomerListPaymentMethodsParams.Type.CARD)
                    .build();

            PaymentMethodCollection paymentMethods = stripeCustomer.listPaymentMethods(params);

            List<CustomerResponse.PaymentMethod> paymentMethodList = paymentMethods.getData().stream()
                    .map(pm -> {
                        PaymentMethod.Card card = pm.getCard();
                        return CustomerResponse.PaymentMethod.builder()
                                .id(pm.getId())
                                .type(pm.getType())
                                .card(CustomerResponse.PaymentMethod.Card.builder()
                                        .brand(card.getBrand())
                                        .last4(card.getLast4())
                                        .expMonth(card.getExpMonth())
                                        .expYear(card.getExpYear())
                                        .build())
                                .build();
                    })
                    .collect(Collectors.toList());

            return CustomerResponse.builder()
                    .id(stripeCustomer.getId())
                    .email(stripeCustomer.getEmail())
                    .name(stripeCustomer.getName())
                    .defaultPaymentMethodId(customer.getDefaultPaymentMethodId())
                    .paymentMethods(paymentMethodList)
                    .createdAt(stripeCustomer.getCreated())
                    .build();
        } catch (StripeException e) {
            log.error("Failed to get customer details for {}: {}", sanitize.sanitizeLogging(customer.getStripeCustomerId()), e.getMessage());
            throw e;
        }
    }
}
