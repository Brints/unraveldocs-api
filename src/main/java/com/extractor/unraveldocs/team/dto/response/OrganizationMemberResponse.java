package com.extractor.unraveldocs.team.dto.response;

import com.extractor.unraveldocs.team.datamodel.OrganizationMemberRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationMemberResponse {
    private String id;
    private String memberId;
    private String userId;
    private String firstName;
    private String lastName;
    private String email;
    private String profilePicture;
    private OrganizationMemberRole role;
    private OffsetDateTime joinedAt;
    private String invitedByName;

    /**
     * Masks an email address for privacy.
     * Example: "john.doe@example.com" -> "j***e@e***e.com"
     *
     * @param email         The email to mask
     * @param isOwnerOrSelf Whether the viewer is the owner or viewing their own
     *                      details
     * @return The masked or original email
     */
    public static String maskEmail(String email, boolean isOwnerOrSelf) {
        if (isOwnerOrSelf || email == null || email.isBlank()) {
            return email;
        }

        try {
            String[] parts = email.split("@");
            if (parts.length != 2) {
                return "***@***.***";
            }

            String localPart = parts[0];
            String domainPart = parts[1];

            // Mask local part: first char + *** + last char
            String maskedLocal;
            if (localPart.length() <= 2) {
                maskedLocal = localPart.charAt(0) + "***";
            } else {
                maskedLocal = "" + localPart.charAt(0) + "***" + localPart.charAt(localPart.length() - 1);
            }

            // Mask domain: first char of domain name + *** + last char + extension
            String[] domainParts = domainPart.split("\\.");
            if (domainParts.length < 2) {
                return maskedLocal + "@***.***";
            }

            String domainName = domainParts[0];
            String extension = domainParts[domainParts.length - 1];

            String maskedDomain;
            if (domainName.length() <= 2) {
                maskedDomain = domainName.charAt(0) + "***";
            } else {
                maskedDomain = "" + domainName.charAt(0) + "***" + domainName.charAt(domainName.length() - 1);
            }

            return maskedLocal + "@" + maskedDomain + "." + extension;
        } catch (Exception e) {
            return "***@***.***";
        }
    }

    /**
     * Creates a response with email masking applied based on viewer permissions
     */
    public OrganizationMemberResponse withMaskedEmail(boolean isOwnerOrSelf) {
        return OrganizationMemberResponse.builder()
                .id(this.id)
                .memberId(this.memberId)
                .userId(this.userId)
                .firstName(this.firstName)
                .lastName(this.lastName)
                .email(maskEmail(this.email, isOwnerOrSelf))
                .profilePicture(this.profilePicture)
                .role(this.role)
                .joinedAt(this.joinedAt)
                .invitedByName(this.invitedByName)
                .build();
    }
}
