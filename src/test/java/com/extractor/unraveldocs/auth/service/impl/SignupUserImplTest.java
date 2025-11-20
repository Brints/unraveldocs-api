package com.extractor.unraveldocs.auth.service.impl;

import com.extractor.unraveldocs.auth.dto.SignupData;
import com.extractor.unraveldocs.auth.dto.request.SignupRequestDto;
import com.extractor.unraveldocs.auth.datamodel.Role;
import com.extractor.unraveldocs.auth.events.UserRegisteredEvent;
import com.extractor.unraveldocs.auth.impl.SignupUserImpl;
import com.extractor.unraveldocs.auth.mappers.UserEventMapper;
import com.extractor.unraveldocs.auth.mappers.UserMapper;
import com.extractor.unraveldocs.auth.mappers.UserVerificationMapper;
import com.extractor.unraveldocs.auth.model.UserVerification;
import com.extractor.unraveldocs.config.RabbitMQConfig;
import com.extractor.unraveldocs.events.BaseEvent;
import com.extractor.unraveldocs.events.EventPublisherService;
import com.extractor.unraveldocs.exceptions.custom.BadRequestException;
import com.extractor.unraveldocs.exceptions.custom.ConflictException;
import com.extractor.unraveldocs.shared.response.ResponseBuilderService;
import com.extractor.unraveldocs.shared.response.UnravelDocsResponse;
import com.extractor.unraveldocs.loginattempts.model.LoginAttempts;
import com.extractor.unraveldocs.subscription.impl.AssignSubscriptionService;
import com.extractor.unraveldocs.subscription.model.UserSubscription;
import com.extractor.unraveldocs.user.model.User;
import com.extractor.unraveldocs.user.repository.UserRepository;
import com.extractor.unraveldocs.utils.generatetoken.GenerateVerificationToken;
import com.extractor.unraveldocs.utils.userlib.DateHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SignupUserImplTest {

    @Mock
    private ResponseBuilderService responseBuilder;
    @Mock
    private DateHelper dateHelper;
    @Mock
    private GenerateVerificationToken verificationToken;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AssignSubscriptionService assignSubscriptionService;
    @Mock
    private UserMapper userMapper;
    @Mock
    private UserVerificationMapper userVerificationMapper;
    @Mock
    private EventPublisherService eventPublisherService;
    @Mock
    private UserEventMapper userEventMapper;

    @InjectMocks
    private SignupUserImpl signupUserService;

    private SignupRequestDto request;
    private User user;
    private UserVerification userVerification;
    private OffsetDateTime expiryDate;

    @BeforeEach
    void setUp() {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.initSynchronization();
        }
        OffsetDateTime now = OffsetDateTime.now();
        expiryDate = now.plusHours(3);

        request = new SignupRequestDto(
                "john",
                "doe",
                "john.doe@example.com",
                "P@ssw0rd123",
                "P@ssw0rd123",
                true,
                true,
                "Engineer",
                "Tech Company",
                "USA"
        );

        user = new User();
        user.setId("1");
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setEmail("john.doe@example.com");
        user.setVerified(false);
        user.setActive(false);

        userVerification = new UserVerification();
        userVerification.setUser(user);
    }

    @AfterEach
    void tearDown() {
        TransactionSynchronizationManager.clear();
    }

    private void setupCommonMocks() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userMapper.toUser(request)).thenReturn(user);
        when(verificationToken.generateVerificationToken()).thenReturn("verificationToken");
        when(dateHelper.setExpiryDate(any(OffsetDateTime.class), eq("hour"), eq(3))).thenReturn(expiryDate);
        when(userVerificationMapper.toUserVerification(any(User.class), anyString(), any(OffsetDateTime.class))).thenReturn(userVerification);
        when(assignSubscriptionService.assignDefaultSubscription(any(User.class))).thenReturn(new UserSubscription());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        when(responseBuilder.buildUserResponse(any(SignupData.class), eq(HttpStatus.CREATED), eq("User registered successfully")))
                .thenAnswer(invocation -> {
                    SignupData data = invocation.getArgument(0);
                    return new UnravelDocsResponse<>(HttpStatus.CREATED.value(), "success", "User registered successfully", data);
                });
    }

    @Test
    void registerUser_SuccessfulRegistration_ReturnsSignupResponse() {
        // Arrange
        setupCommonMocks();
        when(dateHelper.getTimeLeftToExpiry(any(OffsetDateTime.class), any(OffsetDateTime.class), eq("hour"))).thenReturn("3");
        when(userEventMapper.toUserRegisteredEvent(any(User.class), anyString(), anyString())).thenReturn(mock(UserRegisteredEvent.class));


        // Act
        UnravelDocsResponse<SignupData> response = signupUserService.registerUser(request);
        List<TransactionSynchronization> synchronizations = TransactionSynchronizationManager.getSynchronizations();
        assertFalse(synchronizations.isEmpty());
        synchronizations.getFirst().afterCommit();

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.CREATED.value(), response.getStatusCode());
        assertEquals("User registered successfully", response.getMessage());
        assertNotNull(response.getData());
        assertEquals("John", response.getData().firstName());
        assertEquals(Role.USER, response.getData().role());

        verify(userRepository).existsByEmail("john.doe@example.com");
        verify(userRepository).save(argThat(savedUser -> savedUser.getRole() == Role.USER));
        verify(eventPublisherService)
                .publishEvent(eq(RabbitMQConfig.USER_EVENTS_EXCHANGE),
                        eq(RabbitMQConfig.USER_REGISTERED_ROUTING_KEY),
                        any(BaseEvent.class)
                );
    }

    @Test
    void registerUser_FirstUser_SetsSuperAdminRole() {
        // Arrange
        setupCommonMocks();
        when(userRepository.count()).thenReturn(0L);


        // Act
        UnravelDocsResponse<SignupData> response = signupUserService.registerUser(request);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.CREATED.value(), response.getStatusCode());
        assertEquals(Role.SUPER_ADMIN, response.getData().role());
        verify(userRepository).save(argThat(savedUser -> savedUser.getRole() == Role.SUPER_ADMIN));
    }

    @Test
    void registerUser_EmailAlreadyExists_ThrowsConflictException() {
        // Arrange
        when(userRepository.existsByEmail("john.doe@example.com")).thenReturn(true);

        // Act & Assert
        assertThrows(ConflictException.class, () -> signupUserService.registerUser(request), "Email already exists");
        verify(userRepository).existsByEmail("john.doe@example.com");
        verifyNoMoreInteractions(userRepository);
        verifyNoInteractions(userMapper, verificationToken, dateHelper, responseBuilder, assignSubscriptionService);
    }

    @Test
    void registerUser_PasswordSameAsEmail_ThrowsBadRequestException() {
        // Arrange
        SignupRequestDto invalidRequest = new SignupRequestDto("john", "doe", "john.doe@example.com", "john.doe@example.com", "john.doe@example.com", true, false, "Engineer", "Tech", "USA");
        when(userRepository.existsByEmail(anyString())).thenReturn(false);

        // Act & Assert
        assertThrows(BadRequestException.class, () -> signupUserService.registerUser(invalidRequest), "Password cannot be same as email.");
        verify(userRepository).existsByEmail("john.doe@example.com");
        verifyNoMoreInteractions(userRepository);
        verifyNoInteractions(userMapper, verificationToken, dateHelper, responseBuilder, assignSubscriptionService);
    }

    @Test
    void registerUser_VerificationDetailsAndLoginAttemptsSetCorrectly() {
        // Arrange
        setupCommonMocks();

        // Act
        signupUserService.registerUser(request);

        // Assert
        verify(userRepository).save(argThat(u -> {
            UserVerification verification = u.getUserVerification();
            boolean verificationCorrect = verification != null &&
                    verification.getUser() == u;

            LoginAttempts loginAttempts = u.getLoginAttempts();
            boolean loginAttemptsCorrect = loginAttempts != null &&
                    loginAttempts.getUser() == u &&
                    loginAttempts.getLoginAttempts() == 0 &&
                    !loginAttempts.isBlocked();

            return verificationCorrect && loginAttemptsCorrect;
        }));
        verify(userVerificationMapper).toUserVerification(user, "verificationToken", expiryDate);
    }
}
