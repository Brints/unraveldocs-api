package com.extractor.unraveldocs.auth.impl;

import com.extractor.unraveldocs.auth.dto.request.ResendEmailVerificationDto;
import com.extractor.unraveldocs.auth.datamodel.VerifiedStatus;
import com.extractor.unraveldocs.auth.events.UserRegisteredEvent;
import com.extractor.unraveldocs.auth.events.WelcomeEvent;
import com.extractor.unraveldocs.auth.interfaces.EmailVerificationService;
import com.extractor.unraveldocs.auth.model.UserVerification;
import com.extractor.unraveldocs.brokers.kafka.events.BaseEvent;
import com.extractor.unraveldocs.brokers.kafka.events.EventMetadata;
import com.extractor.unraveldocs.brokers.kafka.events.EventPublisherService;
import com.extractor.unraveldocs.brokers.kafka.events.EventTypes;
import com.extractor.unraveldocs.exceptions.custom.BadRequestException;
import com.extractor.unraveldocs.shared.response.UnravelDocsResponse;
import com.extractor.unraveldocs.user.model.User;
import com.extractor.unraveldocs.user.repository.UserRepository;
import com.extractor.unraveldocs.shared.response.ResponseBuilderService;
import com.extractor.unraveldocs.utils.generatetoken.GenerateVerificationToken;
import com.extractor.unraveldocs.utils.userlib.DateHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerificationImpl implements EmailVerificationService {
    private final UserRepository userRepository;
    private final GenerateVerificationToken verificationToken;
    private final DateHelper dateHelper;
    private final ResponseBuilderService responseBuilder;
    private final EventPublisherService eventPublisherService;

    private static final String RESEND_GENERIC_MESSAGE = "If an account with this email exists and is unverified, a verification email has been sent.";

    @Override
    @Transactional
    public UnravelDocsResponse<Void> resendEmailVerification(ResendEmailVerificationDto request) {
        // Issue #3: Prevent user enumeration — always return generic 200 OK
        Optional<User> userOpt = userRepository.findByEmail(request.email());

        if (userOpt.isEmpty()) {
            // User not found — return generic success to prevent enumeration
            return responseBuilder.buildUserResponse(null, HttpStatus.OK, RESEND_GENERIC_MESSAGE);
        }

        User user = userOpt.get();

        if (user.isVerified()) {
            // Already verified — return generic success to prevent enumeration
            return responseBuilder.buildUserResponse(null, HttpStatus.OK, RESEND_GENERIC_MESSAGE);
        }

        UserVerification userVerification = user.getUserVerification();
        OffsetDateTime now = OffsetDateTime.now();

        // Issue #5: Do not leak token TTL — return generic message
        if (userVerification.getEmailVerificationToken() != null &&
                userVerification.getEmailVerificationTokenExpiry().isAfter(now)) {
            return responseBuilder.buildUserResponse(
                    null, HttpStatus.OK,
                    "Please wait before requesting a new verification email.");
        }

        String emailVerificationToken = verificationToken.generateVerificationToken();
        OffsetDateTime emailVerificationTokenExpiry = dateHelper.setExpiryDate(now, "hour", 3);

        userVerification.setEmailVerificationToken(emailVerificationToken);
        userVerification.setEmailVerificationTokenExpiry(emailVerificationTokenExpiry);
        userVerification.setStatus(VerifiedStatus.PENDING);

        userRepository.save(user);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                String expirationText = dateHelper.getTimeLeftToExpiry(now, emailVerificationTokenExpiry, "hour");
                publishVerificationEmailEvent(user, emailVerificationToken, expirationText);
            }
        });

        return responseBuilder.buildUserResponse(null, HttpStatus.OK, RESEND_GENERIC_MESSAGE);
    }

    private void publishVerificationEmailEvent(User user, String token, String expiration) {
        UserRegisteredEvent payload = UserRegisteredEvent.builder()
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .verificationToken(token)
                .expiration(expiration)
                .build();
        EventMetadata metadata = EventMetadata.builder()
                .eventType(EventTypes.USER_REGISTERED)
                .eventSource("EmailVerificationImpl")
                .eventTimestamp(System.currentTimeMillis())
                .correlationId(UUID.randomUUID().toString())
                .build();
        BaseEvent<UserRegisteredEvent> event = new BaseEvent<>(metadata, payload);

        eventPublisherService.publishUserEvent(event);
    }

    @Override
    @Transactional
    public UnravelDocsResponse<Void> verifyEmail(String email, String token) {
        // Issue #3: Use generic error for all failure cases — no 404 to prevent
        // enumeration
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException(
                        "Email verification failed.", "VERIFICATION_FAILED"));

        if (user.isVerified()) {
            throw new BadRequestException(
                    "User is already verified. Please login.", "EMAIL_ALREADY_VERIFIED");
        }

        UserVerification userVerification = user.getUserVerification();
        if (userVerification.getEmailVerificationToken() == null
                || !userVerification.getEmailVerificationToken().equals(token)) {
            throw new BadRequestException(
                    "Email verification failed.", "VERIFICATION_FAILED");
        }

        if (userVerification.getEmailVerificationTokenExpiry().isBefore(OffsetDateTime.now())) {
            userVerification.setStatus(VerifiedStatus.EXPIRED);
            userRepository.save(user);
            throw new BadRequestException(
                    "Email verification token has expired.", "TOKEN_EXPIRED");
        }

        userVerification.setEmailVerificationToken(null);
        userVerification.setEmailVerified(true);
        userVerification.setEmailVerificationTokenExpiry(null);
        userVerification.setStatus(VerifiedStatus.VERIFIED);

        user.setVerified(true);
        user.setActive(true);

        User updatedUser = userRepository.save(user);

        // Registering a synchronization to publish the event after commit
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                WelcomeEvent welcomeEvent = new WelcomeEvent(
                        updatedUser.getEmail(),
                        updatedUser.getFirstName(),
                        updatedUser.getLastName());
                EventMetadata metadata = EventMetadata.builder()
                        .eventType(EventTypes.WELCOME_EVENT)
                        .eventSource("EmailVerificationImpl")
                        .eventTimestamp(System.currentTimeMillis())
                        .correlationId(UUID.randomUUID().toString())
                        .build();
                BaseEvent<WelcomeEvent> event = new BaseEvent<>(metadata, welcomeEvent);
                eventPublisherService.publishUserEvent(event);
            }
        });

        return responseBuilder.buildUserResponse(
                null, HttpStatus.OK, "Email verified successfully");
    }
}