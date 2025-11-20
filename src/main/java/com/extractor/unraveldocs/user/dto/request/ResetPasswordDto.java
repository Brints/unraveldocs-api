package com.extractor.unraveldocs.user.dto.request;

import com.extractor.unraveldocs.auth.dto.PasswordMatches;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
@PasswordMatches(
        passwordField = "newPassword",
        confirmPasswordField = "confirmNewPassword"
)
public record ResetPasswordDto(
        @NotBlank(message = "New Password cannot be empty.")
        @Size(min = 8, message = "New Password must be at least 8 characters long.")
        @Pattern.List({
                @Pattern(regexp = ".*[a-z].*", message = "New Password must contain at least one lowercase letter."),
                @Pattern(regexp = ".*[A-Z].*", message = "New Password must contain at least one uppercase letter."),
                @Pattern(regexp = ".*[0-9].*", message = "New Password must contain at least one digit."),
                @Pattern(regexp = ".*[@$!%*?&].*", message = "New Password must contain at least one special character.")
        })
        String newPassword,

        @NotBlank(message = "Confirm Password cannot be blank")
        String confirmNewPassword,

        @NotBlank(message = "Token cannot be blank")
        String token,

        @NotBlank(message = "Email cannot be blank")
        @Email(message = "Email should be valid")
        String email
) {
}
