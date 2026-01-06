package com.extractor.unraveldocs.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Builder;

/**
 * DTO for updating profile fields via JSON request.
 * Does not include profile picture - use the separate upload endpoint for that.
 */
@Builder
@Schema(description = "Update Profile Request (JSON)", title = "Update Profile Request")
public record ProfileUpdateRequestDto(
                @Schema(description = "First Name of the User", example = "John") @Size(min = 2, max = 80, message = "First name must be between 2 and 80 characters.") String firstName,

                @Schema(description = "Last Name of the User", example = "Doe") @Size(min = 2, max = 80, message = "Last name must be between 2 and 80 characters.") String lastName,

                @Schema(description = "Country of the User", example = "US") @Size(max = 100, message = "Country must be at most 100 characters.") String country,

                @Schema(description = "Profession of the User", example = "Software Engineer") @Size(max = 150, message = "Profession must be at most 150 characters.") String profession,

                @Schema(description = "Organization of the User", example = "Acme Inc.") @Size(max = 200, message = "Organization must be at most 200 characters.") String organization) {
        @Override
        @SuppressWarnings("NullableProblems")
        public String toString() {
                return "ProfileUpdateRequestDto{" +
                                "firstName='" + firstName + '\'' +
                                ", lastName='" + lastName + '\'' +
                                ", country='" + country + '\'' +
                                ", profession='" + profession + '\'' +
                                ", organization='" + organization + '\'' +
                                '}';
        }
}
