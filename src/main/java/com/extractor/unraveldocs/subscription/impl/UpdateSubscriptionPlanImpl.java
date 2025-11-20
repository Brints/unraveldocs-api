package com.extractor.unraveldocs.subscription.impl;

import com.extractor.unraveldocs.exceptions.custom.BadRequestException;
import com.extractor.unraveldocs.exceptions.custom.NotFoundException;
import com.extractor.unraveldocs.shared.response.ResponseBuilderService;
import com.extractor.unraveldocs.shared.response.UnravelDocsResponse;
import com.extractor.unraveldocs.subscription.dto.request.UpdateSubscriptionPlanRequest;
import com.extractor.unraveldocs.subscription.dto.response.SubscriptionPlansData;
import com.extractor.unraveldocs.subscription.datamodel.SubscriptionCurrency;
import com.extractor.unraveldocs.subscription.interfaces.UpdateSubscriptionPlanService;
import com.extractor.unraveldocs.subscription.model.SubscriptionPlan;
import com.extractor.unraveldocs.subscription.repository.SubscriptionPlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.extractor.unraveldocs.subscription.impl.AddSubscriptionPlansImpl.getSubscriptionPlansData;

@Slf4j
@Service
@RequiredArgsConstructor
public class UpdateSubscriptionPlanImpl implements UpdateSubscriptionPlanService {
    private final ResponseBuilderService responseBuilderService;
    private final SubscriptionPlanRepository planRepository;

    @Override
    @Transactional
    public UnravelDocsResponse<SubscriptionPlansData> updateSubscriptionPlan(
            UpdateSubscriptionPlanRequest request, String planId) {
        SubscriptionPlan plan = planRepository.findPlanById(planId)
                .orElseThrow(() -> new NotFoundException("Subscription plan not found with ID: " + planId));

        if (request.newPlanPrice() != null) {
            plan.setPrice(request.newPlanPrice());
        }

        if (request.newPlanCurrency() != null && !request.newPlanCurrency().getFullName().isEmpty()) {
            if (!SubscriptionCurrency.isValidCurrency(request.newPlanCurrency())) {
                throw new BadRequestException("Invalid currency provided. Valid options are: " +
                        String.join(", ", SubscriptionCurrency.getAllValidCurrencies()));
            }
            plan.setCurrency(request.newPlanCurrency());
        }

        if (request.billingIntervalUnit() != null) {
            plan.setBillingIntervalUnit(request.billingIntervalUnit());
        }

        if (request.billingIntervalValue() != null) {
            plan.setBillingIntervalValue(request.billingIntervalValue());
        }

        if (request.documentUploadLimit() != null) {
            plan.setDocumentUploadLimit(request.documentUploadLimit());
        }

        if (request.ocrPageLimit() != null) {
            plan.setOcrPageLimit(request.ocrPageLimit());
        }

        SubscriptionPlan updatedPlan = planRepository.save(plan);

        SubscriptionPlansData plansData = getSubscriptionPlansData(updatedPlan);

        return responseBuilderService
                .buildUserResponse(plansData, HttpStatus.OK, "Subscription plan updated successfully.");
    }
}
