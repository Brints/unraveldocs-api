package com.extractor.unraveldocs.auth.impl;

import com.extractor.unraveldocs.auth.dto.request.ResendEmailVerificationDto;
import com.extractor.unraveldocs.auth.datamodel.VerifiedStatus;
import com.extractor.unraveldocs.auth.events.UserRegisteredEvent;
import com.extractor.unraveldocs.auth.events.WelcomeEvent;
import com.extractor.unraveldocs.auth.interfaces.EmailVerificationService;
import com.extractor.unraveldocs.auth.model.UserVerification;
import com.extractor.unraveldocs.brokers.rabbitmq.config.RabbitMQQueueConfig;
import com.extractor.unraveldocs.brokers.rabbitmq.events.BaseEvent;
import com.extractor.unraveldocs.brokers.rabbitmq.events.EventMetadata;
import com.extractor.unraveldocs.brokers.rabbitmq.events.EventPublisherService;
import com.extractor.unraveldocs.brokers.rabbitmq.events.EventTypes;
import com.extractor.unraveldocs.exceptions.custom.BadRequestException;
import com.extractor.unraveldocs.exceptions.custom.NotFoundException;
import com.extractor.unraveldocs.shared.response.UnravelDocsResponse;
import com.extractor.unraveldocs.user.model.User;
import com.extractor.unraveldocs.user.repository.UserRepository;
import com.extractor.unraveldocs.shared.response.ResponseBuilderService;
import com.extractor.unraveldocs.utils.generatetoken.GenerateVerificationToken;
import com.extractor.unraveldocs.utils.userlib.DateHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmailVerificationImpl implements EmailVerificationService {
    private final UserRepository userRepository;
    private final GenerateVerificationToken verificationToken;
    private final DateHelper dateHelper;
    private final ResponseBuilderService responseBuilder;
    private final EventPublisherService eventPublisherService;

    @Override
    @Transactional
    public UnravelDocsResponse<Void> resendEmailVerification(ResendEmailVerificationDto request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new NotFoundException("User does not exist."));

        if (user.isVerified()) {
            throw new BadRequestException("User is already verified. Please login.");
        }

        UserVerification userVerification = user.getUserVerification();
        OffsetDateTime now = OffsetDateTime.now();

        // Allow resend only if the token is expired or doesn't exist
        if (userVerification.getEmailVerificationToken() != null &&
                userVerification.getEmailVerificationTokenExpiry().isAfter(now)) {
            String timeLeft = dateHelper.getTimeLeftToExpiry(now, userVerification.getEmailVerificationTokenExpiry(),
                    "hour");
            throw new BadRequestException(
                    "A verification email has already been sent. Please check your inbox or try again in " + timeLeft);
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

        return responseBuilder.buildUserResponse(
                null, HttpStatus.OK, "A new verification email has been sent successfully.");
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

        eventPublisherService.publishEvent(
                RabbitMQQueueConfig.USER_EVENTS_EXCHANGE,
                "user.verification.resent",
                event);
    }

    @Override
    @Transactional
    public UnravelDocsResponse<Void> verifyEmail(String email, String token) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User does not exist."));

        if (user.isVerified()) {
            throw new BadRequestException("User is already verified. Please login.");
        }

        UserVerification userVerification = user.getUserVerification();
        if (userVerification.getEmailVerificationToken() == null
                || !userVerification.getEmailVerificationToken().equals(token)) {
            throw new BadRequestException("Invalid email verification token.");
        }

        if (userVerification.getEmailVerificationTokenExpiry().isBefore(OffsetDateTime.now())) {
            userVerification.setStatus(VerifiedStatus.EXPIRED);
            userRepository.save(user);
            throw new BadRequestException("Email verification token has expired.");
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
                eventPublisherService.publishEvent(
                        RabbitMQQueueConfig.USER_EVENTS_EXCHANGE,
                        "user.welcome",
                        event);
            }
        });

        return responseBuilder.buildUserResponse(
                null, HttpStatus.OK, "Email verified successfully");
    }
}