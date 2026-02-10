package com.extractor.unraveldocs.user.dto.request;

import com.extractor.unraveldocs.auth.dto.PasswordMatches;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
@PasswordMatches(message = "New password and confirm new password do not match", passwordField = "newPassword", confirmPasswordField = "confirmNewPassword")
public record ChangePasswordDto(
                @NotBlank(message = "Old password cannot be blank") String oldPassword,

                @NotBlank(message = "New password cannot be blank")
                @Size(min = 8, message = "New password must be at least 8 characters long")
                @Pattern(regexp = ".*[a-z].*", message = "New password must contain at least one lowercase letter")
                @Pattern(regexp = ".*[A-Z].*", message = "New password must contain at least one uppercase letter")
                @Pattern(regexp = ".*[0-9].*", message = "New password must contain at least one digit")
                @Pattern(regexp = ".*[@$!%*?&].*", message = "New password must contain at least one special character")
                String newPassword,

                @NotBlank(message = "Confirm new password cannot be blank") String confirmNewPassword) {
}
