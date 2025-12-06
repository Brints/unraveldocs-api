package com.extractor.unraveldocs.admin.dto.request;

import com.extractor.unraveldocs.auth.dto.PasswordMatches;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
@Schema(description = "Create Admin Request DTO")
@PasswordMatches
public class AdminSignupRequestDto {
    @Schema(description = "First name of the admin user", example = "John")
    @NotNull(message = "First name is required")
    @Size(min = 3, max = 80, message = "First name must be between 3 and 80 characters")
    private String firstName;

    @Schema(description = "Last name of the admin user", example = "Doe")
    @NotNull(message = "Last name is required")
    @Size(min = 3, max = 80, message = "Last name must be between 3 and 80 characters")
    private String lastName;

    @Schema(description = "Email address of the admin user", example = "johndoe@example.com")
    @NotNull(message = "Email is required")
    @Email(message = "Please enter a valid email address")
    @Pattern(
            regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$",
            message = "Email must be a valid email address format"
    )
    @Size(max = 100, message = "Email must be less than 100 characters")
    private String email;

    @Schema(description = "Password of the admin user", example = "P@ssw0rd123")
    @NotNull(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    @Pattern.List({
            @Pattern(regexp = ".*[a-z].*", message = "Password must contain at least one lowercase letter"),
            @Pattern(regexp = ".*[A-Z].*", message = "Password must contain at least one uppercase letter"),
            @Pattern(regexp = ".*[0-9].*", message = "Password must contain at least one digit"),
            @Pattern(regexp = ".*[@$!%*?&].*", message = "Password must contain at least one special character")
    })
    private String password;

    @Schema(description = "Confirm password of the admin user", example = "P@ssw0rd123")
    @NotNull(message = "Confirm password is required")
    private String confirmPassword;

    @Schema(description = "Admin creation code", example = "ADMIN-CREATE-2024")
    @NotNull(message = "Admin creation code is required")
    private String code;

    @Schema(description = "Country of the user", example = "USA")
    @NotNull(message = "Country is required")
    String country;

    @Schema(description = "Terms and Conditions acceptance", example = "true")
    @NotNull(message = "You must accept the terms and conditions")
    @AssertTrue(message = "You must accept the terms and conditions")
    Boolean acceptTerms;

    @Schema(description = "Marketing emails subscription", example = "false")
    Boolean subscribeToMarketing;
}
