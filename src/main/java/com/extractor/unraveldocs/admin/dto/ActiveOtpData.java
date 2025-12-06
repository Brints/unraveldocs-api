package com.extractor.unraveldocs.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ActiveOtpData {
    private String id;
    private String otpCode;
    private String createdBy;
    private boolean isUsed;
    private String usedAt;
    private boolean isExpired;
    private String expiresAt;
    private String createdAt;
}
