package com.extractor.unraveldocs.admin.service.impl;

import com.extractor.unraveldocs.admin.dto.response.SecurityStatsDto;
import com.extractor.unraveldocs.admin.interfaces.AdminSecurityStatsService;
import com.extractor.unraveldocs.auth.datamodel.Role;
import com.extractor.unraveldocs.loginattempts.repository.LoginAttemptsRepository;
import com.extractor.unraveldocs.shared.response.ResponseBuilderService;
import com.extractor.unraveldocs.shared.response.UnravelDocsResponse;
import com.extractor.unraveldocs.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminSecurityStatsServiceImpl implements AdminSecurityStatsService {

    private final UserRepository userRepository;
    private final LoginAttemptsRepository loginAttemptsRepository;
    private final ResponseBuilderService responseBuilderService;

    @Override
    @Transactional(readOnly = true)
    public UnravelDocsResponse<SecurityStatsDto> getSecurityStats() {
        log.info("Fetching admin security statistics");

        long activeBans = loginAttemptsRepository.countByIsBlockedTrue();
        long twoFactorEnabledUsers = 0; // Not yet implemented in the user model

        LocalDateTime twentyFourHoursAgo = LocalDateTime.now().minusHours(24);
        long recentFailedLogins = loginAttemptsRepository.sumFailedLoginsSince(twentyFourHoursAgo);

        List<Object[]> rolesRaw = userRepository.countByRoleGrouped();
        Map<String, Long> userRoles = new LinkedHashMap<>();
        for (Object[] row : rolesRaw) {
            String roleName = ((Role) row[0]).name();
            Long count = ((Number) row[1]).longValue();
            userRoles.put(roleName, count);
        }

        SecurityStatsDto stats = SecurityStatsDto.builder()
                .activeBans(activeBans)
                .twoFactorEnabledUsers(twoFactorEnabledUsers)
                .recentFailedLogins(recentFailedLogins)
                .userRoles(userRoles)
                .build();

        return responseBuilderService.buildUserResponse(
                stats,
                HttpStatus.OK,
                "Security stats retrieved successfully"
        );
    }
}
