package com.extractor.unraveldocs.admin.service.impl;

import com.extractor.unraveldocs.admin.dto.request.ActionReasonDto;
import com.extractor.unraveldocs.admin.dto.request.UpdatePlanLimitsDto;
import com.extractor.unraveldocs.exceptions.custom.NotFoundException;
import com.extractor.unraveldocs.shared.response.UnravelDocsResponse;
import com.extractor.unraveldocs.subscription.datamodel.SubscriptionPlans;
import com.extractor.unraveldocs.subscription.model.SubscriptionPlan;
import com.extractor.unraveldocs.subscription.repository.SubscriptionPlanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminPlanManagementServiceImplTest {

    @Mock
    private SubscriptionPlanRepository planRepository;

    @InjectMocks
    private AdminPlanManagementServiceImpl testClass;

    private SubscriptionPlan mockPlan;

    @BeforeEach
    void setUp() {
        mockPlan = new SubscriptionPlan();
        mockPlan.setId("plan-123");
        mockPlan.setName(SubscriptionPlans.PRO_MONTHLY);
        mockPlan.setDocumentUploadLimit(100);
        mockPlan.setOcrPageLimit(50);
        mockPlan.setStorageLimit(1073741824L); // 1GB
        mockPlan.setAiOperationsLimit(500);
        mockPlan.setPrice(BigDecimal.valueOf(9.99));
        mockPlan.setTrialDays(14);
        mockPlan.setActive(true);
    }

    @Test
    void getAllPlans_returnsPlanList() {
        when(planRepository.findAll()).thenReturn(List.of(mockPlan));

        UnravelDocsResponse<List<SubscriptionPlan>> response = testClass.getAllPlans();

        assertNotNull(response);
        assertEquals(200, response.getStatusCode());
        assertEquals(1, response.getData().size());
        assertEquals("plan-123", response.getData().getFirst().getId());
    }

    @Test
    void updatePlanLimits_success() {
        UpdatePlanLimitsDto req = new UpdatePlanLimitsDto(200, 100, 2147483648L, 1000, BigDecimal.valueOf(19.99), 30);
        when(planRepository.findById("plan-123")).thenReturn(Optional.of(mockPlan));

        UnravelDocsResponse<String> response = testClass.updatePlanLimits("plan-123", req);

        assertEquals(200, response.getStatusCode());
        verify(planRepository).save(mockPlan);
        assertEquals(200, mockPlan.getDocumentUploadLimit());
        assertEquals(30, mockPlan.getTrialDays());
        assertEquals(BigDecimal.valueOf(19.99), mockPlan.getPrice());
    }

    @Test
    void updatePlanLimits_throwsNotFound() {
        UpdatePlanLimitsDto req = new UpdatePlanLimitsDto();
        when(planRepository.findById("unknown")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> testClass.updatePlanLimits("unknown", req));
        verify(planRepository, never()).save(any());
    }

    @Test
    void togglePlanStatus_deactivatesPlan() {
        when(planRepository.findById("plan-123")).thenReturn(Optional.of(mockPlan));

        UnravelDocsResponse<String> response = testClass.togglePlanStatus("plan-123", false, new ActionReasonDto("Temporarily disabled"));

        assertEquals(200, response.getStatusCode());
        assertFalse(mockPlan.isActive());
        verify(planRepository).save(mockPlan);
    }

    @Test
    void togglePlanStatus_noActionIfAlreadySameState() {
        when(planRepository.findById("plan-123")).thenReturn(Optional.of(mockPlan));

        UnravelDocsResponse<String> response = testClass.togglePlanStatus("plan-123", true, new ActionReasonDto("Already active"));

        assertEquals(200, response.getStatusCode());
        assertTrue(mockPlan.isActive());
        verify(planRepository, never()).save(any());
    }
}
