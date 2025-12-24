package com.extractor.unraveldocs.elasticsearch.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import java.time.OffsetDateTime;

/**
 * Elasticsearch document for indexing payment and receipt data.
 * Used for admin payment search functionality.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "payments")
public class PaymentSearchIndex {

    @Id
    private String id;

    /**
     * User ID who made the payment.
     */
    @Field(type = FieldType.Keyword)
    private String userId;

    /**
     * User's email for quick reference.
     */
    @Field(type = FieldType.Keyword)
    private String userEmail;

    /**
     * User's full name.
     */
    @MultiField(mainField = @Field(type = FieldType.Text, analyzer = "standard"), otherFields = {
            @InnerField(suffix = "keyword", type = FieldType.Keyword)
    })
    private String userName;

    /**
     * Receipt number for lookup.
     */
    @Field(type = FieldType.Keyword)
    private String receiptNumber;

    /**
     * Payment provider (STRIPE, PAYSTACK, etc.).
     */
    @Field(type = FieldType.Keyword)
    private String paymentProvider;

    /**
     * External payment ID from provider.
     */
    @Field(type = FieldType.Keyword)
    private String externalPaymentId;

    /**
     * Payment type (ONE_TIME, SUBSCRIPTION).
     */
    @Field(type = FieldType.Keyword)
    private String paymentType;

    /**
     * Payment status.
     */
    @Field(type = FieldType.Keyword)
    private String status;

    /**
     * Payment amount (stored as Double for Elasticsearch compatibility).
     */
    @Field(type = FieldType.Scaled_Float, scalingFactor = 100)
    private Double amount;

    /**
     * Currency code (USD, NGN, etc.).
     */
    @Field(type = FieldType.Keyword)
    private String currency;

    /**
     * Payment method used.
     */
    @Field(type = FieldType.Keyword)
    private String paymentMethod;

    /**
     * Payment method details (masked card number, etc.).
     */
    @Field(type = FieldType.Keyword)
    private String paymentMethodDetails;

    /**
     * Payment description (searchable).
     */
    @Field(type = FieldType.Text, analyzer = "standard")
    private String description;

    /**
     * Subscription plan name if applicable.
     */
    @Field(type = FieldType.Keyword)
    private String subscriptionPlan;

    /**
     * Receipt URL.
     */
    @Field(type = FieldType.Keyword, index = false)
    private String receiptUrl;

    /**
     * Whether receipt email was sent.
     */
    @Field(type = FieldType.Boolean)
    private Boolean emailSent;

    /**
     * Timestamp when payment was made.
     */
    @Field(type = FieldType.Date, format = DateFormat.date_optional_time)
    private OffsetDateTime paidAt;

    /**
     * Record creation timestamp.
     */
    @Field(type = FieldType.Date, format = DateFormat.date_optional_time)
    private OffsetDateTime createdAt;

    /**
     * Last update timestamp.
     */
    @Field(type = FieldType.Date, format = DateFormat.date_optional_time)
    private OffsetDateTime updatedAt;
}
