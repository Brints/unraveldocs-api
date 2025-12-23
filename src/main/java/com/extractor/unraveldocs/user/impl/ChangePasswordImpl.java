package com.extractor.unraveldocs.user.impl;

import com.extractor.unraveldocs.brokers.rabbitmq.config.RabbitMQQueueConfig;
import com.extractor.unraveldocs.brokers.rabbitmq.events.BaseEvent;
import com.extractor.unraveldocs.brokers.rabbitmq.events.EventMetadata;
import com.extractor.unraveldocs.brokers.rabbitmq.events.EventPublisherService;
import com.extractor.unraveldocs.brokers.rabbitmq.events.EventTypes;
import com.extractor.unraveldocs.exceptions.custom.BadRequestException;
import com.extractor.unraveldocs.exceptions.custom.ForbiddenException;
import com.extractor.unraveldocs.exceptions.custom.NotFoundException;
import com.extractor.unraveldocs.security.JwtTokenProvider;
import com.extractor.unraveldocs.security.TokenBlacklistService;
import com.extractor.unraveldocs.shared.response.ResponseBuilderService;
import com.extractor.unraveldocs.shared.response.UnravelDocsResponse;
import com.extractor.unraveldocs.user.dto.request.ChangePasswordDto;
import com.extractor.unraveldocs.user.events.PasswordChangedEvent;
import com.extractor.unraveldocs.user.interfaces.userimpl.ChangePasswordService;
import com.extractor.unraveldocs.user.model.User;
import com.extractor.unraveldocs.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChangePasswordImpl implements ChangePasswordService {

    private final PasswordEncoder passwordEncoder;
    private final ResponseBuilderService responseBuilder;
    private final TokenBlacklistService tokenBlacklist;
    private final JwtTokenProvider tokenProvider;
    private final UserRepository userRepository;
    private final EventPublisherService eventPublisherService;

    @Override
    @Transactional
    public UnravelDocsResponse<Void> changePassword(ChangePasswordDto request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User does not exist."));

        if (!user.isVerified()) {
            throw new ForbiddenException("Account not verified. Please verify your account first.");
        }

        if (!passwordEncoder.matches(request.oldPassword(), user.getPassword())) {
            throw new BadRequestException("Old password is incorrect.");
        }

        if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {
            throw new BadRequestException("New password cannot be the same as the old password.");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        String jti;
        if (authentication.getCredentials() instanceof String token) {
            jti = tokenProvider.getJtiFromToken(token);
            if (jti != null) {
                tokenBlacklist.blacklistToken(jti, tokenProvider.getAccessExpirationInMs());
            }
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                publishPasswordChangedEvent(user);
            }
        });

        return responseBuilder
                .buildUserResponse(null, HttpStatus.OK, "Password changed successfully.");
    }

    private void publishPasswordChangedEvent(User user) {
        PasswordChangedEvent payload = PasswordChangedEvent.builder()
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .build();
        EventMetadata metadata = EventMetadata.builder()
                .eventType(EventTypes.PASSWORD_CHANGED)
                .eventSource("ChangePasswordImpl")
                .eventTimestamp(System.currentTimeMillis())
                .correlationId(UUID.randomUUID().toString())
                .build();
        BaseEvent<PasswordChangedEvent> event = new BaseEvent<>(metadata, payload);

        eventPublisherService.publishEvent(
                RabbitMQQueueConfig.USER_EVENTS_EXCHANGE,
                "user.password.changed",
                event);
    }
}