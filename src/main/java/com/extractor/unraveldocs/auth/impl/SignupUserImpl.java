package com.extractor.unraveldocs.auth.impl;

import com.extractor.unraveldocs.auth.dto.SignupData;
import com.extractor.unraveldocs.auth.dto.request.SignupRequestDto;
import com.extractor.unraveldocs.auth.datamodel.Role;
import com.extractor.unraveldocs.auth.datamodel.VerifiedStatus;
import com.extractor.unraveldocs.auth.interfaces.SignupUserService;
import com.extractor.unraveldocs.auth.model.UserVerification;
import com.extractor.unraveldocs.credit.service.CreditBalanceService;
import com.extractor.unraveldocs.brokers.kafka.events.BaseEvent;
import com.extractor.unraveldocs.brokers.kafka.events.EventMetadata;
import com.extractor.unraveldocs.brokers.kafka.events.EventPublisherService;
import com.extractor.unraveldocs.auth.events.UserRegisteredEvent;
import com.extractor.unraveldocs.brokers.kafka.events.EventTypes;
import com.extractor.unraveldocs.elasticsearch.events.IndexAction;
import com.extractor.unraveldocs.elasticsearch.service.ElasticsearchIndexingService;
import com.extractor.unraveldocs.exceptions.custom.BadRequestException;
import com.extractor.unraveldocs.exceptions.custom.ConflictException;
import com.extractor.unraveldocs.pushnotification.datamodel.NotificationType;
import com.extractor.unraveldocs.pushnotification.interfaces.NotificationService;
import com.extractor.unraveldocs.shared.response.ResponseBuilderService;
import com.extractor.unraveldocs.shared.response.UnravelDocsResponse;
import com.extractor.unraveldocs.loginattempts.model.LoginAttempts;
import com.extractor.unraveldocs.subscription.impl.AssignSubscriptionService;
import com.extractor.unraveldocs.subscription.model.UserSubscription;
import com.extractor.unraveldocs.user.model.User;
import com.extractor.unraveldocs.user.repository.UserRepository;
import com.extractor.unraveldocs.utils.generatetoken.GenerateVerificationToken;
import com.extractor.unraveldocs.utils.userlib.DateHelper;
import com.extractor.unraveldocs.utils.userlib.UserLibrary;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SignupUserImpl implements SignupUserService {
        private final AssignSubscriptionService assignSubscriptionService;
        private final DateHelper dateHelper;
        private final EventPublisherService eventPublisherService;
        private final GenerateVerificationToken verificationToken;
        private final PasswordEncoder passwordEncoder;
        private final ResponseBuilderService responseBuilder;
        private final UserLibrary userLibrary;
        private final UserRepository userRepository;
        private final Optional<ElasticsearchIndexingService> elasticsearchIndexingService;
        private final NotificationService notificationService;
        private final CreditBalanceService creditBalanceService;

        @Override
        @Transactional
        public UnravelDocsResponse<SignupData> registerUser(SignupRequestDto request) {
                if (userRepository.existsByEmail(request.email())) {
                        throw new ConflictException("Email already exists");
                }

                if (request.password().equalsIgnoreCase(request.email())) {
                        throw new BadRequestException("Password cannot be same as email.");
                }

                OffsetDateTime now = OffsetDateTime.now();

                // Create new User
                User user = new User();
                user.setFirstName(userLibrary.capitalizeFirstLetterOfName(request.firstName()));
                user.setLastName(userLibrary.capitalizeFirstLetterOfName(request.lastName()));
                user.setEmail(request.email().toLowerCase());
                user.setPassword(passwordEncoder.encode(request.password()));
                user.setActive(false);
                user.setVerified(false);
                user.setRole(Role.USER);
                user.setTermsAccepted(request.acceptTerms() != null ? request.acceptTerms() : false);
                user.setMarketingOptIn(request.subscribeToMarketing() != null ? request.subscribeToMarketing() : false);
                user.setCountry(request.country());
                user.setProfession(request.profession());
                user.setOrganization(request.organization());
                user.setCreatedAt(now);
                user.setUpdatedAt(now);

                String emailVerificationToken = verificationToken.generateVerificationToken();
                OffsetDateTime emailVerificationTokenExpiry = dateHelper.setExpiryDate(now, "hour", 3);

                // Create UserVerification and associate with User
                UserVerification userVerification = new UserVerification();
                userVerification.setUser(user);
                userVerification.setEmailVerificationToken(emailVerificationToken);
                userVerification.setEmailVerificationTokenExpiry(emailVerificationTokenExpiry);
                userVerification.setStatus(VerifiedStatus.PENDING);
                userVerification.setEmailVerified(false);
                user.setUserVerification(userVerification);

                // Initialize LoginAttempts for the User
                LoginAttempts loginAttempts = new LoginAttempts();
                loginAttempts.setUser(user);
                user.setLoginAttempts(loginAttempts);

                // Assign default subscription based on user role
                UserSubscription subscription = assignSubscriptionService.assignDefaultSubscription(user);
                user.setSubscription(subscription);

                User savedUser = userRepository.save(user);

                // Registering a synchronization to publish the event after commit
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                                String expiration = dateHelper
                                                .getTimeLeftToExpiry(now, emailVerificationTokenExpiry, "hour");
                                UserRegisteredEvent payload = UserRegisteredEvent.builder()
                                                .email(savedUser.getEmail())
                                                .firstName(savedUser.getFirstName())
                                                .lastName(savedUser.getLastName())
                                                .verificationToken(emailVerificationToken)
                                                .expiration(expiration)
                                                .build();

                                EventMetadata metadata = EventMetadata.builder()
                                                .eventType(EventTypes.USER_REGISTERED)
                                                .eventSource("SignupUserImpl")
                                                .eventTimestamp(System.currentTimeMillis())
                                                .correlationId(UUID.randomUUID().toString())
                                                .build();

                                BaseEvent<UserRegisteredEvent> event = new BaseEvent<>(metadata, payload);

                                eventPublisherService.publishUserEvent(event);

                                // Index user in Elasticsearch
                                elasticsearchIndexingService
                                                .ifPresent(service -> service.indexUser(savedUser, IndexAction.CREATE));

                                // Send WELCOME push notification
                                sendWelcomeNotification(savedUser);

                                // Grant sign-up bonus credits
                                try {
                                        creditBalanceService.grantSignupBonus(savedUser);
                                        log.info("Granted sign-up bonus credits to user {}", savedUser.getId());
                                } catch (Exception e) {
                                        log.error("Failed to grant sign-up bonus credits to user {}: {}",
                                                        savedUser.getId(), e.getMessage());
                                }
                        }
                });

                // Prepare response data
                SignupData signupData = SignupData.builder()
                                .id(savedUser.getId())
                                .profilePicture(savedUser.getProfilePicture())
                                .firstName(savedUser.getFirstName())
                                .lastName(savedUser.getLastName())
                                .email(savedUser.getEmail())
                                .role(savedUser.getRole())
                                .lastLogin(savedUser.getLastLogin())
                                .isActive(savedUser.isActive())
                                .isVerified(savedUser.isVerified())
                                .termsAccepted(savedUser.isTermsAccepted())
                                .marketingOptIn(savedUser.isMarketingOptIn())
                                .isPlatformAdmin(savedUser.isPlatformAdmin())
                                .isOrganizationAdmin(savedUser.isOrganizationAdmin())
                                .country(savedUser.getCountry())
                                .profession(savedUser.getProfession())
                                .organization(savedUser.getOrganization())
                                .createdAt(savedUser.getCreatedAt())
                                .updatedAt(savedUser.getUpdatedAt())
                                .build();

                return responseBuilder
                                .buildUserResponse(
                                                signupData,
                                                HttpStatus.CREATED,
                                                "User registered successfully");
        }

        private void sendWelcomeNotification(User user) {
                try {
                        Map<String, String> data = new HashMap<>();
                        data.put("userId", user.getId());
                        data.put("email", user.getEmail());

                        notificationService.sendToUser(
                                        user.getId(),
                                        NotificationType.WELCOME,
                                        "Welcome to UnravelDocs!",
                                        String.format("Hi %s, welcome to UnravelDocs! Start uploading documents to get started.",
                                                        user.getFirstName()),
                                        data);
                        log.debug("Sent welcome notification to user {}", user.getId());
                } catch (Exception e) {
                        log.error("Failed to send welcome notification: {}", e.getMessage());
                }
        }
}