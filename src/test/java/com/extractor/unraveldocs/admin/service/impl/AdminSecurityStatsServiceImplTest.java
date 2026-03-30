package com.extractor.unraveldocs.admin.service.impl;

import com.extractor.unraveldocs.admin.dto.response.SecurityStatsDto;
import com.extractor.unraveldocs.auth.datamodel.Role;
import com.extractor.unraveldocs.loginattempts.repository.LoginAttemptsRepository;
import com.extractor.unraveldocs.shared.response.ResponseBuilderService;
import com.extractor.unraveldocs.shared.response.UnravelDocsResponse;
import com.extractor.unraveldocs.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminSecurityStatsServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private LoginAttemptsRepository loginAttemptsRepository;

    @Mock
    private ResponseBuilderService responseBuilderService;

    @InjectMocks
    private AdminSecurityStatsServiceImpl adminSecurityStatsService;

    @BeforeEach
    void setUp() {
    }

    @Test
    void getSecurityStats_Success() {
        // Arrange
        when(loginAttemptsRepository.countByIsBlockedTrue()).thenReturn(15L);
        when(loginAttemptsRepository.sumFailedLoginsSince(any(LocalDateTime.class))).thenReturn(45L);

        List<Object[]> rolesRaw = new ArrayList<>();
        rolesRaw.add(new Object[]{Role.USER, 1000L});
        rolesRaw.add(new Object[]{Role.ADMIN, 5L});
        rolesRaw.add(new Object[]{Role.SUPER_ADMIN, 2L});
        when(userRepository.countByRoleGrouped()).thenReturn(rolesRaw);

        SecurityStatsDto expectedStats = SecurityStatsDto.builder()
                .activeBans(15L)
                .twoFactorEnabledUsers(0L)
                .recentFailedLogins(45L)
                .userRoles(Map.of(
                        "USER", 1000L,
                        "ADMIN", 5L,
                        "SUPER_ADMIN", 2L
                ))
                .build();

        UnravelDocsResponse<SecurityStatsDto> expectedResponse = new UnravelDocsResponse<>(
                200,
                "success",
                "Security stats retrieved successfully",
                expectedStats
        );

        when(responseBuilderService.buildUserResponse(
                any(SecurityStatsDto.class),
                eq(HttpStatus.OK),
                eq("Security stats retrieved successfully")
        )).thenReturn(expectedResponse);

        // Act
        UnravelDocsResponse<SecurityStatsDto> response = adminSecurityStatsService.getSecurityStats();

        // Assert
        assertNotNull(response);
        assertEquals(200, response.getStatusCode());
        assertEquals("success", response.getStatus());

        SecurityStatsDto data = response.getData();
        assertNotNull(data);
        assertEquals(15L, data.getActiveBans());
        assertEquals(0L, data.getTwoFactorEnabledUsers());
        assertEquals(45L, data.getRecentFailedLogins());
        
        Map<String, Long> userRoles = data.getUserRoles();
        assertNotNull(userRoles);
        assertEquals(1000L, userRoles.get("USER"));
        assertEquals(5L, userRoles.get("ADMIN"));
        assertEquals(2L, userRoles.get("SUPER_ADMIN"));
    }
}
