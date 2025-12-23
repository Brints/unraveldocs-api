package com.extractor.unraveldocs.admin.impl;

import com.extractor.unraveldocs.admin.dto.AdminData;
import com.extractor.unraveldocs.admin.dto.request.AdminSignupRequestDto;
import com.extractor.unraveldocs.admin.events.AdminCreatedEvent;
import com.extractor.unraveldocs.admin.interfaces.CreateAdminService;
import com.extractor.unraveldocs.admin.repository.OtpRepository;
import com.extractor.unraveldocs.auth.datamodel.Role;
import com.extractor.unraveldocs.auth.model.UserVerification;
import com.extractor.unraveldocs.brokers.rabbitmq.config.RabbitMQQueueConfig;
import com.extractor.unraveldocs.brokers.rabbitmq.events.BaseEvent;
import com.extractor.unraveldocs.brokers.rabbitmq.events.EventMetadata;
import com.extractor.unraveldocs.brokers.rabbitmq.events.EventPublisherService;
import com.extractor.unraveldocs.brokers.rabbitmq.events.EventTypes;
import com.extractor.unraveldocs.exceptions.custom.BadRequestException;
import com.extractor.unraveldocs.exceptions.custom.ConflictException;
import com.extractor.unraveldocs.loginattempts.model.LoginAttempts;
import com.extractor.unraveldocs.shared.response.ResponseBuilderService;
import com.extractor.unraveldocs.shared.response.UnravelDocsResponse;
import com.extractor.unraveldocs.subscription.impl.AssignSubscriptionService;
import com.extractor.unraveldocs.subscription.model.UserSubscription;
import com.extractor.unraveldocs.user.model.User;
import com.extractor.unraveldocs.user.repository.UserRepository;
import com.extractor.unraveldocs.utils.generatetoken.GenerateVerificationToken;
import com.extractor.unraveldocs.utils.userlib.DateHelper;
import com.extractor.unraveldocs.utils.userlib.UserLibrary;
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
public class CreateAdminUserImpl implements CreateAdminService {
    private final AssignSubscriptionService subscription;
    private final DateHelper dateHelper;
    private final EventPublisherService eventPublisher;
    private final GenerateVerificationToken generateVerificationToken;
    private final ResponseBuilderService responseBuilder;
    private final UserRepository userRepository;
    private final UserLibrary library;
    private final OtpRepository otpRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public UnravelDocsResponse<AdminData> createAdminUser(AdminSignupRequestDto request) {
        // Check if code is a valid code
        var code = request.getCode();
        if (code == null || code.isBlank()) {
            throw new ConflictException("Admin signup code is required.");
        }

        // check if code exists and is valid
        var otpRecord = otpRepository.findByOtpCodeAndIsExpiredFalseAndIsUsedFalse(code)
                .orElseThrow(() -> new ConflictException("Invalid or expired admin signup code."));

        OffsetDateTime now = OffsetDateTime.now();

        // check if otp is expired
        if (otpRecord.getExpiresAt().isBefore(now)) {
            otpRecord.setExpired(true);
            otpRepository.save(otpRecord);
            throw new BadRequestException("Admin signup code has expired.");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Admin user already exists with email: " + request.getEmail());
        }

        if (request.getPassword().equalsIgnoreCase(request.getEmail())) {
            throw new ConflictException("Password cannot be same as email.");
        }

        User newAdmin = new User();
        newAdmin.setFirstName(library.capitalizeFirstLetterOfName(request.getFirstName()));
        newAdmin.setLastName(library.capitalizeFirstLetterOfName(request.getLastName()));
        newAdmin.setEmail(request.getEmail());
        newAdmin.setPassword(passwordEncoder.encode(request.getPassword()));
        newAdmin.setActive(true);
        newAdmin.setVerified(true);
        newAdmin.setRole(Role.ADMIN);
        newAdmin.setPlatformAdmin(true);
        newAdmin.setCountry(request.getCountry());
        newAdmin.setTermsAccepted(request.getAcceptTerms());
        newAdmin.setMarketingOptIn(request.getSubscribeToMarketing());
        newAdmin.setCreatedAt(now);
        newAdmin.setUpdatedAt(now);

        String emailVerificationToken = generateVerificationToken.generateVerificationToken();
        OffsetDateTime tokenExpiry = dateHelper.setExpiryDate(now, "hour", 3);

        UserVerification userVerification = new UserVerification();
        userVerification.setUser(newAdmin);
        userVerification.setEmailVerificationToken(emailVerificationToken);
        userVerification.setEmailVerificationTokenExpiry(tokenExpiry);
        newAdmin.setUserVerification(userVerification);

        // Initialize login attempts for the new admin user
        LoginAttempts loginAttempts = new LoginAttempts();
        loginAttempts.setUser(newAdmin);
        newAdmin.setLoginAttempts(loginAttempts);

        // Assign default subscription based on user role
        UserSubscription adminSubscription = subscription.assignDefaultSubscription(newAdmin);
        newAdmin.setSubscription(adminSubscription);

        User savedAdmin = userRepository.save(newAdmin);

        // Mark the OTP as used
        otpRecord.setUsed(true);
        otpRecord.setUsedAt(OffsetDateTime.now());
        otpRepository.save(otpRecord);

        // Publish admin created event
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                String expiration = dateHelper
                        .getTimeLeftToExpiry(now, tokenExpiry, "hour");
                AdminCreatedEvent payload = new AdminCreatedEvent();
                payload.setId(savedAdmin.getId());
                payload.setEmail(newAdmin.getEmail());
                payload.setFirstName(newAdmin.getFirstName());
                payload.setLastName(newAdmin.getLastName());
                payload.setToken(emailVerificationToken);
                payload.setTokenExpiry(expiration);

                EventMetadata metadata = EventMetadata.builder()
                        .eventType(EventTypes.ADMIN_CREATED)
                        .eventSource("CreateAdminUserImpl")
                        .eventTimestamp(System.currentTimeMillis())
                        .correlationId(UUID.randomUUID().toString())
                        .build();

                BaseEvent<AdminCreatedEvent> event = new BaseEvent<>(metadata, payload);

                eventPublisher.publishEvent(
                        RabbitMQQueueConfig.ADMIN_EVENTS_EXCHANGE,
                        RabbitMQQueueConfig.ADMIN_CREATED_ROUTING_KEY,
                        event
                );
            }
        });

        AdminData adminData = new AdminData();
        adminData.setEmail(newAdmin.getEmail());
        adminData.setFirstName(newAdmin.getFirstName());
        adminData.setLastName(newAdmin.getLastName());
        adminData.setRole(Role.ADMIN);

        return responseBuilder.buildUserResponse(adminData, HttpStatus.CREATED, "Admin user created successfully");
    }
}
