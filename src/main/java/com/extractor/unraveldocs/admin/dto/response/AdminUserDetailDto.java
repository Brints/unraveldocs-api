package com.extractor.unraveldocs.admin.dto.response;

import com.extractor.unraveldocs.auth.datamodel.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserDetailDto {
    private Profile profile;
    private StatusFlags statusFlags;
    private Subscription subscription;
    private UsageQuotas usageQuotas;
    private CreditBalance creditBalance;
    private LoginSecurity loginSecurity;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Profile {
        private String id;
        private String name;
        private String email;
        private String profilePicture;
        private String country;
        private String profession;
        private String organization;
        private Role role;
        private OffsetDateTime createdAt;
        private OffsetDateTime lastLogin;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class StatusFlags {
        private boolean isActive;
        private boolean isVerified;
        private boolean isPlatformAdmin;
        private boolean isOrganizationAdmin;
        private boolean termsAccepted;
        private boolean marketingOptIn;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Subscription {
        private String planName;
        private String status;
        private OffsetDateTime periodStart;
        private OffsetDateTime periodEnd;
        private Boolean autoRenew;
        private OffsetDateTime trialEndsAt;
        private String source;
        private String gatewaySubscriptionId;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class UsageQuotas {
        private Long storageUsed;
        private Long storageLimit;
        private Integer ocrUsed;
        private Integer ocrLimit;
        private Integer aiUsed;
        private Integer aiLimit;
        private Integer docsUploaded;
        private Integer docsLimit;
        private OffsetDateTime quotaResetDate;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class CreditBalance {
        private int balance;
        private int totalPurchased;
        private int totalUsed;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class LoginSecurity {
        private int attempts;
        private boolean isBlocked;
        private OffsetDateTime blockedUntil;
    }
}
