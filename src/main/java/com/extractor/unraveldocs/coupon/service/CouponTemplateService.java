package com.extractor.unraveldocs.coupon.service;

import com.extractor.unraveldocs.coupon.dto.CouponTemplateData;
import com.extractor.unraveldocs.coupon.dto.request.CreateCouponTemplateRequest;
import com.extractor.unraveldocs.coupon.dto.response.CouponListData;
import com.extractor.unraveldocs.shared.response.UnravelDocsResponse;
import com.extractor.unraveldocs.user.model.User;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Service interface for coupon template operations.
 */
public interface CouponTemplateService {

    /**
     * Creates a new coupon template.
     */
    UnravelDocsResponse<CouponTemplateData> createTemplate(CreateCouponTemplateRequest request, User createdBy);

    /**
     * Updates an existing template.
     */
    UnravelDocsResponse<CouponTemplateData> updateTemplate(
            String templateId,
            CreateCouponTemplateRequest request,
            User updatedBy);

    /**
     * Deactivates a template.
     */
    UnravelDocsResponse<Void> deactivateTemplate(String templateId, User user);

    /**
     * Gets a template by ID.
     */
    UnravelDocsResponse<CouponTemplateData> getTemplateById(String templateId);

    /**
     * Gets all templates with pagination.
     */
    UnravelDocsResponse<List<CouponTemplateData>> getAllTemplates(int page, int size, Boolean isActive);

    /**
     * Creates a coupon from a template.
     */
    UnravelDocsResponse<CouponListData> createCouponFromTemplate(
            String templateId,
            String customCode,
            OffsetDateTime validFrom,
            OffsetDateTime validUntil,
            Boolean sendNotifications,
            User createdBy);
}
