package com.extractor.unraveldocs.auth.impl;

import com.extractor.unraveldocs.auth.dto.SignupData;
import com.extractor.unraveldocs.auth.dto.request.SignUpRequestDto;
import com.extractor.unraveldocs.auth.datamodel.Role;
import com.extractor.unraveldocs.auth.interfaces.SignupUserService;
import com.extractor.unraveldocs.auth.mappers.UserEventMapper;
import com.extractor.unraveldocs.auth.mappers.UserMapper;
import com.extractor.unraveldocs.auth.mappers.UserVerificationMapper;
import com.extractor.unraveldocs.auth.model.UserVerification;
import com.extractor.unraveldocs.events.BaseEvent;
import com.extractor.unraveldocs.events.EventMetadata;
import com.extractor.unraveldocs.events.EventPublisherService;
import com.extractor.unraveldocs.auth.events.UserRegisteredEvent;
import com.extractor.unraveldocs.exceptions.custom.BadRequestException;
import com.extractor.unraveldocs.exceptions.custom.ConflictException;
import com.extractor.unraveldocs.shared.response.ResponseBuilderService;
import com.extractor.unraveldocs.shared.response.UnravelDocsDataResponse;
import com.extractor.unraveldocs.loginattempts.model.LoginAttempts;
import com.extractor.unraveldocs.subscription.impl.AssignSubscriptionService;
import com.extractor.unraveldocs.subscription.model.UserSubscription;
import com.extractor.unraveldocs.user.model.User;
import com.extractor.unraveldocs.user.repository.UserRepository;
import com.extractor.unraveldocs.utils.generatetoken.GenerateVerificationToken;
import com.extractor.unraveldocs.utils.userlib.DateHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SignupUserImpl implements SignupUserService {
    private final AssignSubscriptionService assignSubscriptionService;
    private final DateHelper dateHelper;
    private final EventPublisherService eventPublisherService;
    private final GenerateVerificationToken verificationToken;
    private final ResponseBuilderService responseBuilder;
    private final UserEventMapper userEventMapper;
    private final UserMapper userMapper;
    private final UserRepository userRepository;
    private final UserVerificationMapper userVerificationMapper;

    @Override
    @Transactional
    @CacheEvict(value = "superAdminExists", allEntries = true)
    public UnravelDocsDataResponse<SignupData> registerUser(SignUpRequestDto request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException("Email already exists");
        }

        if (request.password().equalsIgnoreCase(request.email())) {
            throw new BadRequestException("Password cannot be same as email.");
        }

        User user = userMapper.toUser(request);

        OffsetDateTime now = OffsetDateTime.now();
        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        boolean noSuperAdmin = userRepository.superAdminExists();
        user.setRole(noSuperAdmin ? Role.SUPER_ADMIN : Role.USER);

        String emailVerificationToken = verificationToken.generateVerificationToken();
        OffsetDateTime emailVerificationTokenExpiry = dateHelper.setExpiryDate(now,"hour", 3);

        UserVerification userVerification = userVerificationMapper.toUserVerification(user, emailVerificationToken, emailVerificationTokenExpiry);
        user.setUserVerification(userVerification);

        LoginAttempts loginAttempts = new LoginAttempts();
        loginAttempts.setUser(user);
        user.setLoginAttempts(loginAttempts);

        // Assign default subscription based on user role
        UserSubscription subscription = assignSubscriptionService.assignDefaultSubscription(user);
        user.setSubscription(subscription);

        User savedUser = userRepository.save(user);

        // TODO: Implement email sending service
        String expiration = dateHelper.getTimeLeftToExpiry(now, emailVerificationTokenExpiry, "hour");
        UserRegisteredEvent payload = userEventMapper.toUserRegisteredEvent(savedUser, emailVerificationToken, expiration);

        EventMetadata metadata = EventMetadata.builder()
                .eventType("UserRegisteredEvent")
                .eventSource("SignupUserImpl")
                .eventTimestamp(System.currentTimeMillis())
                .correlationId(UUID.randomUUID().toString())
                .userId(savedUser.getId())
                .build();

        BaseEvent<UserRegisteredEvent> event = BaseEvent.<UserRegisteredEvent>builder()
                .metadata(metadata)
                .payload(payload)
                .build();

        eventPublisherService.publishEvent(
                "user.events.exchange",
                "user.registered",
                event
        );

        SignupData signupData = SignupData.builder()
                .id(user.getId())
                .profilePicture(user.getProfilePicture())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .role(user.getRole())
                .lastLogin(user.getLastLogin())
                .isActive(user.isActive())
                .isVerified(user.isVerified())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();

        return responseBuilder.buildUserResponse(signupData, HttpStatus.CREATED, "User registered successfully");
    }
}
