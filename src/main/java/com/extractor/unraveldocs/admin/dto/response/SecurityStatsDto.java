package com.extractor.unraveldocs.admin.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecurityStatsDto {
    private long activeBans;
    private long twoFactorEnabledUsers;
    private long recentFailedLogins;
    private Map<String, Long> userRoles;
}
