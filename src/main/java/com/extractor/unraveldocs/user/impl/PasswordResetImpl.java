package com.extractor.unraveldocs.user.impl;

import com.extractor.unraveldocs.auth.datamodel.VerifiedStatus;
import com.extractor.unraveldocs.auth.model.UserVerification;
import com.extractor.unraveldocs.brokers.kafka.events.BaseEvent;
import com.extractor.unraveldocs.brokers.kafka.events.EventMetadata;
import com.extractor.unraveldocs.brokers.kafka.events.EventPublisherService;
import com.extractor.unraveldocs.brokers.kafka.events.EventTypes;
import com.extractor.unraveldocs.exceptions.custom.BadRequestException;
import com.extractor.unraveldocs.exceptions.custom.ForbiddenException;
import com.extractor.unraveldocs.exceptions.custom.NotFoundException;
import com.extractor.unraveldocs.shared.response.ResponseBuilderService;
import com.extractor.unraveldocs.shared.response.UnravelDocsResponse;
import com.extractor.unraveldocs.user.dto.request.ForgotPasswordDto;
import com.extractor.unraveldocs.user.dto.request.ResetPasswordDto;
import com.extractor.unraveldocs.user.events.PasswordResetEvent;
import com.extractor.unraveldocs.user.events.PasswordResetSuccessfulEvent;
import com.extractor.unraveldocs.user.interfaces.passwordreset.IPasswordReset;
import com.extractor.unraveldocs.user.interfaces.userimpl.PasswordResetService;
import com.extractor.unraveldocs.user.model.User;
import com.extractor.unraveldocs.user.repository.UserRepository;
import com.extractor.unraveldocs.utils.generatetoken.GenerateVerificationToken;
import com.extractor.unraveldocs.utils.userlib.DateHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PasswordResetImpl implements PasswordResetService {
    private final DateHelper dateHelper;
    private final GenerateVerificationToken generateVerificationToken;
    private final PasswordEncoder passwordEncoder;
    private final ResponseBuilderService responseBuilder;
    private final UserRepository userRepository;
    private final EventPublisherService eventPublisherService;

    @Override
    @Transactional
    public UnravelDocsResponse<Void> forgotPassword(ForgotPasswordDto forgotPasswordDto) {
        String email = forgotPasswordDto.email();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User does not exist."));

        UserVerification userVerification = user.getUserVerification();

        if (!user.isVerified() || !userVerification.isEmailVerified()) {
            throw new BadRequestException(
                    "This account is not verified. Please verify your account before resetting the password.");
        }

        OffsetDateTime currentTime = OffsetDateTime.now();

        if (userVerification.getPasswordResetToken() != null &&
                userVerification.getPasswordResetTokenExpiry() != null &&
                userVerification.getPasswordResetTokenExpiry().isAfter(currentTime)) {
            String timeLeft = dateHelper.getTimeLeftToExpiry(currentTime,
                    userVerification.getPasswordResetTokenExpiry(), "hours");
            throw new BadRequestException(
                    "A password reset request has already been sent. Please check your email. Token expires in: "
                            + timeLeft);
        }

        String token = generateVerificationToken.generateVerificationToken();
        OffsetDateTime expiryTime = dateHelper.setExpiryDate(currentTime, "hour", 1);

        userVerification.setPasswordResetToken(token);
        userVerification.setPasswordResetTokenExpiry(expiryTime);
        userRepository.save(user);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                String expiration = dateHelper.getTimeLeftToExpiry(currentTime, expiryTime, "hours");
                publishPasswordResetRequestedEvent(user, token, expiration);
            }
        });

        return responseBuilder.buildUserResponse(
                null,
                HttpStatus.OK,
                "Password reset link sent to your email.");
    }

    @Override
    @Transactional
    public UnravelDocsResponse<Void> resetPassword(IPasswordReset params, ResetPasswordDto request) {
        String email = params.getEmail();
        String token = params.getToken();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User does not exist."));

        UserVerification userVerification = user.getUserVerification();

        if (!user.isVerified() || !userVerification.isEmailVerified()) {
            throw new ForbiddenException("Account not verified. Please verify your account first.");
        }

        if (userVerification.getPasswordResetToken() == null
                || !userVerification.getPasswordResetToken().equals(token)) {
            throw new BadRequestException("Invalid password reset token.");
        }

        if (userVerification.getPasswordResetTokenExpiry().isBefore(OffsetDateTime.now())) {
            userVerification.setStatus(VerifiedStatus.EXPIRED);
            userRepository.save(user);
            throw new BadRequestException("Password reset token has expired.");
        }

        if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {
            throw new BadRequestException("New password cannot be the same as the old password.");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userVerification.setPasswordResetToken(null);
        userVerification.setPasswordResetTokenExpiry(null);
        userVerification.setStatus(VerifiedStatus.VERIFIED);
        userRepository.save(user);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                publishPasswordResetSuccessfulEvent(user);
            }
        });

        return responseBuilder.buildUserResponse(
                null,
                HttpStatus.OK,
                "Password reset successfully.");
    }

    private void publishPasswordResetRequestedEvent(User user, String token, String expiration) {
        PasswordResetEvent payload = PasswordResetEvent.builder()
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .token(token)
                .expiration(expiration)
                .build();
        EventMetadata metadata = EventMetadata.builder()
                .eventType(EventTypes.PASSWORD_RESET_REQUESTED)
                .eventSource("PasswordResetImpl")
                .eventTimestamp(System.currentTimeMillis())
                .correlationId(UUID.randomUUID().toString())
                .build();
        BaseEvent<PasswordResetEvent> event = new BaseEvent<>(metadata, payload);

        eventPublisherService.publishUserEvent(event);
    }

    private void publishPasswordResetSuccessfulEvent(User user) {
        PasswordResetSuccessfulEvent payload = PasswordResetSuccessfulEvent.builder()
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .build();
        EventMetadata metadata = EventMetadata.builder()
                .eventType(EventTypes.PASSWORD_RESET_SUCCESSFUL)
                .eventSource("PasswordResetImpl")
                .eventTimestamp(System.currentTimeMillis())
                .correlationId(UUID.randomUUID().toString())
                .build();
        BaseEvent<PasswordResetSuccessfulEvent> event = new BaseEvent<>(metadata, payload);

        eventPublisherService.publishUserEvent(event);
    }
}