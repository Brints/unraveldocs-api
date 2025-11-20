package com.extractor.unraveldocs.subscription.service.impl;

import com.extractor.unraveldocs.exceptions.custom.BadRequestException;
import com.extractor.unraveldocs.shared.response.ResponseBuilderService;
import com.extractor.unraveldocs.shared.response.UnravelDocsResponse;
import com.extractor.unraveldocs.subscription.dto.request.CreateSubscriptionPlanRequest;
import com.extractor.unraveldocs.subscription.dto.response.SubscriptionPlansData;
import com.extractor.unraveldocs.subscription.datamodel.BillingIntervalUnit;
import com.extractor.unraveldocs.subscription.datamodel.SubscriptionCurrency;
import com.extractor.unraveldocs.subscription.datamodel.SubscriptionPlans;
import com.extractor.unraveldocs.subscription.impl.AddSubscriptionPlansImpl;
import com.extractor.unraveldocs.subscription.model.SubscriptionPlan;
import com.extractor.unraveldocs.subscription.repository.SubscriptionPlanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AddSubscriptionPlansImplTest {

    @Mock
    private SubscriptionPlanRepository planRepository;

    @Mock
    private ResponseBuilderService responseBuilderService;

    @InjectMocks
    private AddSubscriptionPlansImpl addSubscriptionPlansService;

    private CreateSubscriptionPlanRequest request;
    private SubscriptionPlan savedPlan;
    private SubscriptionPlansData plansData;

    @BeforeEach
    void setUp() {
        request = new CreateSubscriptionPlanRequest(
                SubscriptionPlans.FREE,
                new BigDecimal("9.99"),
                SubscriptionCurrency.USD,
                BillingIntervalUnit.MONTH,
                1,
                100,
                500
        );

        savedPlan = new SubscriptionPlan();
        savedPlan.setId(String.valueOf(1L));
        savedPlan.setName(request.name());
        savedPlan.setPrice(request.price());
        savedPlan.setCurrency(request.currency());
        savedPlan.setBillingIntervalUnit(request.billingIntervalUnit());
        savedPlan.setBillingIntervalValue(request.billingIntervalValue());
        savedPlan.setDocumentUploadLimit(request.documentUploadLimit());
        savedPlan.setOcrPageLimit(request.ocrPageLimit());
        savedPlan.setActive(true);
        savedPlan.setCreatedAt(OffsetDateTime.now());
        savedPlan.setUpdatedAt(OffsetDateTime.now());

        plansData = AddSubscriptionPlansImpl.getSubscriptionPlansData(savedPlan);
    }

    @Test
    void createSubscriptionPlan_success() {
        // Arrange
        when(planRepository.findByName(request.name())).thenReturn(Optional.empty());
        when(planRepository.save(any(SubscriptionPlan.class))).thenReturn(savedPlan);

        UnravelDocsResponse<SubscriptionPlansData> expectedResponse = new UnravelDocsResponse<>();
        expectedResponse.setData(plansData);
        expectedResponse.setStatusCode(HttpStatus.CREATED.value());
        when(responseBuilderService.buildUserResponse(any(SubscriptionPlansData.class), eq(HttpStatus.CREATED), anyString()))
                .thenReturn(expectedResponse);

        // Act
        UnravelDocsResponse<SubscriptionPlansData> actualResponse = addSubscriptionPlansService.createSubscriptionPlan(request);

        // Assert
        assertNotNull(actualResponse);
        assertEquals(expectedResponse, actualResponse);
        assertEquals(HttpStatus.CREATED.value(), actualResponse.getStatusCode());

        ArgumentCaptor<SubscriptionPlan> planCaptor = ArgumentCaptor.forClass(SubscriptionPlan.class);
        verify(planRepository).save(planCaptor.capture());
        SubscriptionPlan capturedPlan = planCaptor.getValue();

        assertEquals(request.name(), capturedPlan.getName());
        assertEquals(request.price(), capturedPlan.getPrice());
        assertTrue(capturedPlan.isActive());

        verify(planRepository).findByName(request.name());
        verify(responseBuilderService).buildUserResponse(any(SubscriptionPlansData.class), eq(HttpStatus.CREATED), eq("Subscription plan created successfully."));
    }

    @Test
    void createSubscriptionPlan_nameAlreadyExists_throwsBadRequestException() {
        // Arrange
        when(planRepository.findByName(request.name())).thenReturn(Optional.of(new SubscriptionPlan()));

        // Act & Assert
        BadRequestException exception = assertThrows(BadRequestException.class, () ->
            addSubscriptionPlansService.createSubscriptionPlan(request));

        assertEquals("Subscription plan with this name already exists.", exception.getMessage());
        verify(planRepository).findByName(request.name());
        verify(planRepository, never()).save(any());
        verifyNoInteractions(responseBuilderService);
    }

    @Test
    void getSubscriptionPlansData_mapsCorrectly() {
        // Arrange
        // 'savedPlan' is already created in setUp

        // Act
        SubscriptionPlansData resultData = AddSubscriptionPlansImpl.getSubscriptionPlansData(savedPlan);

        // Assert
        assertNotNull(resultData);
        assertEquals(savedPlan.getId(), resultData.getId());
        assertEquals(savedPlan.getName(), resultData.getPlanName());
        assertEquals(savedPlan.getPrice(), resultData.getPlanPrice());
        assertEquals(savedPlan.getCurrency(), resultData.getPlanCurrency());
        assertEquals(savedPlan.getBillingIntervalUnit(), resultData.getBillingIntervalUnit());
        assertEquals(savedPlan.getBillingIntervalValue(), resultData.getBillingIntervalValue());
        assertEquals(savedPlan.getDocumentUploadLimit(), resultData.getDocumentUploadLimit());
        assertEquals(savedPlan.getOcrPageLimit(), resultData.getOcrPageLimit());
        assertEquals(savedPlan.isActive(), resultData.isActive());
        assertEquals(savedPlan.getCreatedAt(), resultData.getCreatedAt());
        assertEquals(savedPlan.getUpdatedAt(), resultData.getUpdatedAt());
    }
}