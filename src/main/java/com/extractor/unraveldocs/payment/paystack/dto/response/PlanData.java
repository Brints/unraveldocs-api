package com.extractor.unraveldocs.payment.paystack.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for Paystack plan data
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PlanData {

    private Long id;

    private String name;

    @JsonProperty("plan_code")
    private String planCode;

    private String description;

    private Long amount;

    private String interval;

    private String currency;

    @JsonProperty("send_invoices")
    private Boolean sendInvoices;

    @JsonProperty("send_sms")
    private Boolean sendSms;

    @JsonProperty("hosted_page")
    private Boolean hostedPage;

    @JsonProperty("hosted_page_url")
    private String hostedPageUrl;

    @JsonProperty("hosted_page_summary")
    private String hostedPageSummary;

    @JsonProperty("is_deleted")
    private Boolean isDeleted;

    @JsonProperty("is_archived")
    private Boolean isArchived;

    @JsonProperty("invoice_limit")
    private Integer invoiceLimit;

    @JsonProperty("created_at")
    private String createdAt;

    @JsonProperty("updated_at")
    private String updatedAt;
}

