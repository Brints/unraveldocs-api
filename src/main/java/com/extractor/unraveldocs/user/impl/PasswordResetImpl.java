package com.extractor.unraveldocs.user.impl;

import com.extractor.unraveldocs.auth.datamodel.VerifiedStatus;
import com.extractor.unraveldocs.auth.model.UserVerification;
import com.extractor.unraveldocs.exceptions.custom.BadRequestException;
import com.extractor.unraveldocs.exceptions.custom.ForbiddenException;
import com.extractor.unraveldocs.exceptions.custom.NotFoundException;
import com.extractor.unraveldocs.messaging.emailtemplates.UserEmailTemplateService;
import com.extractor.unraveldocs.user.dto.request.ForgotPasswordDto;
import com.extractor.unraveldocs.user.dto.request.ResetPasswordDto;
import com.extractor.unraveldocs.shared.response.UnravelDocsDataResponse;
import com.extractor.unraveldocs.user.interfaces.passwordreset.IPasswordReset;
import com.extractor.unraveldocs.user.interfaces.userimpl.PasswordResetService;
import com.extractor.unraveldocs.user.model.User;
import com.extractor.unraveldocs.user.repository.UserRepository;
import com.extractor.unraveldocs.shared.response.ResponseBuilderService;
import com.extractor.unraveldocs.utils.generatetoken.GenerateVerificationToken;
import com.extractor.unraveldocs.utils.userlib.DateHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class PasswordResetImpl implements PasswordResetService {
    private final DateHelper dateHelper;
    private final GenerateVerificationToken generateVerificationToken;
    private final PasswordEncoder passwordEncoder;
    private final ResponseBuilderService responseBuilder;
    private final UserEmailTemplateService userEmailTemplateService;
    private final UserRepository userRepository;

    @Override
    public UnravelDocsDataResponse<Void> forgotPassword(ForgotPasswordDto forgotPasswordDto) {
        String email = forgotPasswordDto.email();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User does not exist."));

        UserVerification userVerification = user.getUserVerification();

        if (!user.isVerified() || !userVerification.isEmailVerified()) {
            throw new BadRequestException("This account is not verified. Please verify your account before resetting the password.");
        }

        if (userVerification.getPasswordResetToken() != null) {
            throw new BadRequestException("A password reset request has already been sent. Please check your email.");
        }

        // Current time
        OffsetDateTime currentTime = OffsetDateTime.now();

        if (
                userVerification.getPasswordResetTokenExpiry() != null &&
                        userVerification.getPasswordResetTokenExpiry().isAfter(currentTime)
        ){
            String timeLeft = dateHelper.getTimeLeftToExpiry(currentTime, userVerification.getPasswordResetTokenExpiry(), "hours");
            throw new BadRequestException(
                    "A password reset request has already been sent. Token expires in : " + timeLeft);
        }

        String token = generateVerificationToken.generateVerificationToken();
        OffsetDateTime expiryTime = dateHelper.setExpiryDate(currentTime,"hour", 1);

        userVerification.setPasswordResetToken(token);
        userVerification.setPasswordResetTokenExpiry(expiryTime);
        userRepository.save(user);

        // TODO: Send email with the token (implementation not shown)
        String expiration = dateHelper.getTimeLeftToExpiry(currentTime, expiryTime, "hours");
        userEmailTemplateService.sendPasswordResetToken(
                email,
                user.getFirstName(),
                user.getLastName(),
                token,
                expiration
        );

        return responseBuilder.buildUserResponse(
                null,
                HttpStatus.OK,
                "Password reset link sent to your email."
        );
    }

    @Override
    public UnravelDocsDataResponse<Void> resetPassword(IPasswordReset params, ResetPasswordDto request) {
        String email = params.getEmail();
        String token = params.getToken();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User does not exist."));

        UserVerification userVerification = user.getUserVerification();

        if (!user.isVerified() || !userVerification.isEmailVerified()) {
            throw new ForbiddenException("Account not verified. Please verify your account first.");
        }

        if (!userVerification.getPasswordResetToken().equals(token)) {
            throw new BadRequestException("Invalid password reset token.");
        }

        if (userVerification.getPasswordResetTokenExpiry().isBefore(OffsetDateTime.now())) {
            userVerification.setStatus(VerifiedStatus.EXPIRED);
            userRepository.save(user);
            throw new BadRequestException("Password reset token has expired.");
        }

        boolean isOldPassword =
                passwordEncoder.matches(request.newPassword(), user.getPassword());
        if (isOldPassword) {
            throw new BadRequestException("New password cannot be the same as the old password.");
        }

        String encodedPassword = passwordEncoder.encode(request.newPassword());
        user.setPassword(encodedPassword);
        userVerification.setPasswordResetToken(null);
        userVerification.setPasswordResetTokenExpiry(null);
        userVerification.setStatus(VerifiedStatus.VERIFIED);
        userRepository.save(user);

        // TODO: Send email to user with the new password (implementation not shown)
        userEmailTemplateService.sendSuccessfulPasswordReset(
                email,
                user.getFirstName(),
                user.getLastName()
        );

        return responseBuilder.buildUserResponse(
                null,
                HttpStatus.OK,
                "Password reset successfully."
        );
    }
}
