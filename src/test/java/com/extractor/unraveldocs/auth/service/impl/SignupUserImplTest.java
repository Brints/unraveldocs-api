package com.extractor.unraveldocs.auth.service.impl;

import com.extractor.unraveldocs.auth.dto.SignupData;
import com.extractor.unraveldocs.auth.dto.request.SignupRequestDto;
import com.extractor.unraveldocs.auth.datamodel.Role;
import com.extractor.unraveldocs.auth.impl.SignupUserImpl;
import com.extractor.unraveldocs.auth.model.UserVerification;
import com.extractor.unraveldocs.brokers.rabbitmq.config.RabbitMQQueueConfig;
import com.extractor.unraveldocs.brokers.rabbitmq.events.BaseEvent;
import com.extractor.unraveldocs.brokers.rabbitmq.events.EventPublisherService;
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
import com.extractor.unraveldocs.utils.userlib.UserLibrary;
import com.extractor.unraveldocs.elasticsearch.service.ElasticsearchIndexingService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

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
        private PasswordEncoder passwordEncoder;
        @Mock
        private UserLibrary userLibrary;
        @Mock
        private EventPublisherService eventPublisherService;
        @Mock
        private ElasticsearchIndexingService elasticsearchIndexingService;

        @InjectMocks
        private SignupUserImpl signupUserService;

        private SignupRequestDto request;
        private OffsetDateTime expiryDate;

        @BeforeEach
        void setUp() {
                if (!TransactionSynchronizationManager.isSynchronizationActive()) {
                        TransactionSynchronizationManager.initSynchronization();
                }
                OffsetDateTime now = OffsetDateTime.now();
                expiryDate = now.plusHours(3);

                // Manually construct with Optional since @InjectMocks doesn't handle
                // Optional<T>
                signupUserService = new SignupUserImpl(
                                assignSubscriptionService,
                                dateHelper,
                                eventPublisherService,
                                verificationToken,
                                passwordEncoder,
                                responseBuilder,
                                userLibrary,
                                userRepository,
                                Optional.of(elasticsearchIndexingService));

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
                                "USA");

                User user = new User();
                user.setId("1");
                user.setFirstName("John");
                user.setLastName("Doe");
                user.setEmail("john.doe@example.com");
                user.setVerified(false);
                user.setActive(false);

                UserVerification userVerification = new UserVerification();
                userVerification.setUser(user);
        }

        @AfterEach
        void tearDown() {
                TransactionSynchronizationManager.clear();
        }

        private void setupCommonMocks() {
                when(userRepository.existsByEmail(anyString())).thenReturn(false);
                when(userLibrary.capitalizeFirstLetterOfName("john")).thenReturn("John");
                when(userLibrary.capitalizeFirstLetterOfName("doe")).thenReturn("Doe");
                when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
                when(verificationToken.generateVerificationToken()).thenReturn("verificationToken");
                when(dateHelper.setExpiryDate(any(OffsetDateTime.class), eq("hour"), eq(3))).thenReturn(expiryDate);
                when(assignSubscriptionService.assignDefaultSubscription(any(User.class)))
                                .thenReturn(new UserSubscription());
                when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

                when(responseBuilder.buildUserResponse(any(SignupData.class), eq(HttpStatus.CREATED),
                                eq("User registered successfully")))
                                .thenAnswer(invocation -> {
                                        SignupData data = invocation.getArgument(0);
                                        return new UnravelDocsResponse<>(HttpStatus.CREATED.value(), "success",
                                                        "User registered successfully", data);
                                });
        }

        @Test
        void registerUser_SuccessfulRegistration_ReturnsSignupResponse() {
                // Arrange
                setupCommonMocks();
                when(dateHelper.getTimeLeftToExpiry(any(OffsetDateTime.class), any(OffsetDateTime.class), eq("hour")))
                                .thenReturn("3");

                // Act
                UnravelDocsResponse<SignupData> response = signupUserService.registerUser(request);
                List<TransactionSynchronization> synchronizations = TransactionSynchronizationManager
                                .getSynchronizations();
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
                                .publishEvent(eq(RabbitMQQueueConfig.USER_EVENTS_EXCHANGE),
                                                eq(RabbitMQQueueConfig.USER_REGISTERED_ROUTING_KEY),
                                                any(BaseEvent.class));
        }

        @Test
        void registerUser_EmailAlreadyExists_ThrowsConflictException() {
                // Arrange
                when(userRepository.existsByEmail("john.doe@example.com")).thenReturn(true);

                // Act & Assert
                assertThrows(ConflictException.class, () -> signupUserService.registerUser(request),
                                "Email already exists");
                verify(userRepository).existsByEmail("john.doe@example.com");
                verifyNoMoreInteractions(userRepository);
                verifyNoInteractions(userLibrary, passwordEncoder, verificationToken, dateHelper, responseBuilder,
                                assignSubscriptionService);
        }

        @Test
        void registerUser_PasswordSameAsEmail_ThrowsBadRequestException() {
                // Arrange
                SignupRequestDto invalidRequest = new SignupRequestDto("john", "doe", "john.doe@example.com",
                                "john.doe@example.com", "john.doe@example.com", true, false, "Engineer", "Tech", "USA");
                when(userRepository.existsByEmail(anyString())).thenReturn(false);

                // Act & Assert
                assertThrows(BadRequestException.class, () -> signupUserService.registerUser(invalidRequest),
                                "Password cannot be same as email.");
                verify(userRepository).existsByEmail("john.doe@example.com");
                verifyNoMoreInteractions(userRepository);
                verifyNoInteractions(userLibrary, passwordEncoder, verificationToken, dateHelper, responseBuilder,
                                assignSubscriptionService);
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
                                        verification.getUser() == u &&
                                        verification.getEmailVerificationToken().equals("verificationToken") &&
                                        verification.getEmailVerificationTokenExpiry().equals(expiryDate);

                        LoginAttempts loginAttempts = u.getLoginAttempts();
                        boolean loginAttemptsCorrect = loginAttempts != null &&
                                        loginAttempts.getUser() == u &&
                                        loginAttempts.getLoginAttempts() == 0 &&
                                        !loginAttempts.isBlocked();

                        return verificationCorrect && loginAttemptsCorrect;
                }));
        }
}
