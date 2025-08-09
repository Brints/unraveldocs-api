package com.extractor.unraveldocs.auth.service.impl;

import com.extractor.unraveldocs.auth.dto.SignupData;
import com.extractor.unraveldocs.auth.dto.request.SignUpRequestDto;
import com.extractor.unraveldocs.auth.datamodel.Role;
import com.extractor.unraveldocs.auth.datamodel.VerifiedStatus;
import com.extractor.unraveldocs.auth.impl.SignupUserImpl;
import com.extractor.unraveldocs.auth.model.UserVerification;
import com.extractor.unraveldocs.exceptions.custom.BadRequestException;
import com.extractor.unraveldocs.exceptions.custom.ConflictException;
import com.extractor.unraveldocs.shared.response.ResponseBuilderService;
import com.extractor.unraveldocs.shared.response.UnravelDocsDataResponse;
import com.extractor.unraveldocs.loginattempts.model.LoginAttempts;
import com.extractor.unraveldocs.messaging.emailtemplates.AuthEmailTemplateService;
import com.extractor.unraveldocs.subscription.impl.AssignSubscriptionService;
import com.extractor.unraveldocs.subscription.model.UserSubscription;
import com.extractor.unraveldocs.user.model.User;
import com.extractor.unraveldocs.user.repository.UserRepository;
import com.extractor.unraveldocs.utils.generatetoken.GenerateVerificationToken;
import com.extractor.unraveldocs.utils.userlib.DateHelper;
import com.extractor.unraveldocs.utils.userlib.UserLibrary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;


import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SignupUserImplTest {

    @Mock
    private AuthEmailTemplateService templatesService;

    @Mock
    private ResponseBuilderService responseBuilder;

    @Mock
    private DateHelper dateHelper;

    @Mock
    private GenerateVerificationToken verificationToken;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserLibrary userLibrary;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AssignSubscriptionService assignSubscriptionService;

    @InjectMocks
    private SignupUserImpl signupUserService;

    private SignUpRequestDto request;
    private OffsetDateTime expiryDate;

    @BeforeEach
    void setUp() {
        OffsetDateTime now = OffsetDateTime.now();
        expiryDate = now.plusHours(3);

        request = new SignUpRequestDto(
                "john",
                "doe",
                "john.doe@example.com",
                "P@ssw0rd123",
                "P@ssw0rd123"
        );
    }

    @Test
    void registerUser_SuccessfulRegistration_ReturnsSignupResponse() {
        // Arrange
        SignupData data = SignupData.builder()
                .id(String.valueOf(1L))
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .profilePicture(null)
                .isVerified(false)
                .isActive(false)
                .role(Role.USER)
                .lastLogin(null)
                .build();

        UnravelDocsDataResponse<SignupData> expectedResponse = new UnravelDocsDataResponse<>();
        expectedResponse.setStatusCode(HttpStatus.CREATED.value());
        expectedResponse.setStatus("success");
        expectedResponse.setMessage("User registered successfully");
        expectedResponse.setData(data);

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userLibrary.capitalizeFirstLetterOfName("john")).thenReturn("John");
        when(userLibrary.capitalizeFirstLetterOfName("doe")).thenReturn("Doe");
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(verificationToken.generateVerificationToken()).thenReturn("verificationToken");
        when(dateHelper.setExpiryDate(any(OffsetDateTime.class), eq("hour"), eq(3))).thenReturn(expiryDate);
        when(dateHelper.getTimeLeftToExpiry(any(OffsetDateTime.class), any(OffsetDateTime.class), eq("hour"))).thenReturn("3");
        when(userRepository.superAdminExists()).thenReturn(false);
        when(assignSubscriptionService.assignDefaultSubscription(any(User.class))).thenReturn(new UserSubscription());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            savedUser.setId(String.valueOf(1L));
            return savedUser;
        });
        when(responseBuilder.buildUserResponse(
                any(SignupData.class), eq(HttpStatus.CREATED), eq("User registered successfully")
        )).thenReturn(expectedResponse);

        // Act
        UnravelDocsDataResponse<SignupData> response = signupUserService.registerUser(request);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.CREATED.value(), response.getStatusCode());
        assertEquals("success", response.getStatus());
        assertEquals("User registered successfully", response.getMessage());
        assertNotNull(response.getData());
        assertEquals("John", response.getData().firstName());
        assertEquals("Doe", response.getData().lastName());
        assertEquals("john.doe@example.com", response.getData().email());
        assertEquals(Role.USER, response.getData().role());
        assertFalse(response.getData().isVerified());
        assertFalse(response.getData().isActive());
        assertNull(response.getData().lastLogin());
        assertNull(response.getData().profilePicture());

        verify(userRepository).existsByEmail("john.doe@example.com");
        verify(userLibrary).capitalizeFirstLetterOfName("john");
        verify(userLibrary).capitalizeFirstLetterOfName("doe");
        verify(passwordEncoder).encode("P@ssw0rd123");
        verify(verificationToken).generateVerificationToken();
        verify(dateHelper).setExpiryDate(any(OffsetDateTime.class), eq("hour"), eq(3));
        verify(assignSubscriptionService).assignDefaultSubscription(any(User.class));
        verify(templatesService).sendVerificationEmail(
                eq("john.doe@example.com"), eq("John"), eq("Doe"), eq("verificationToken"), eq("3"));
        verify(userRepository).save(any(User.class));
        verify(responseBuilder).buildUserResponse(
                any(SignupData.class), eq(HttpStatus.CREATED), eq("User registered successfully")
        );
    }

    @Test
    void registerUser_FirstUser_SetsSuperAdminRole() {
        // Arrange
        SignupData data = SignupData.builder()
                .id(String.valueOf(1L))
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .profilePicture(null)
                .isVerified(false)
                .isActive(false)
                .role(Role.SUPER_ADMIN)
                .lastLogin(null)
                .build();

        UnravelDocsDataResponse<SignupData> expectedResponse = new UnravelDocsDataResponse<>();
        expectedResponse.setStatusCode(HttpStatus.CREATED.value());
        expectedResponse.setStatus("success");
        expectedResponse.setMessage("User registered successfully");
        expectedResponse.setData(data);

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userLibrary.capitalizeFirstLetterOfName(anyString())).thenReturn("John");
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(verificationToken.generateVerificationToken()).thenReturn("verificationToken");
        when(dateHelper.setExpiryDate(any(OffsetDateTime.class), eq("hour"), eq(3))).thenReturn(expiryDate);
        when(dateHelper.getTimeLeftToExpiry(any(OffsetDateTime.class), any(OffsetDateTime.class), eq("hour"))).thenReturn("3");
        when(userRepository.superAdminExists()).thenReturn(true);
        when(assignSubscriptionService.assignDefaultSubscription(any(User.class))).thenReturn(new UserSubscription());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            savedUser.setId(String.valueOf(1L));
            return savedUser;
        });
        when(responseBuilder.buildUserResponse(
                any(SignupData.class), eq(HttpStatus.CREATED), eq("User registered successfully")
        )).thenReturn(expectedResponse);

        // Act
        UnravelDocsDataResponse<SignupData> response = signupUserService.registerUser(request);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.CREATED.value(), response.getStatusCode());
        assertEquals(Role.SUPER_ADMIN, response.getData().role());
        verify(userRepository).superAdminExists();
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
        verifyNoInteractions(userLibrary, passwordEncoder, verificationToken, dateHelper,
                templatesService, responseBuilder, assignSubscriptionService);
    }

    @Test
    void registerUser_PasswordSameAsEmail_ThrowsBadRequestException() {
        // Arrange
        SignUpRequestDto invalidRequest = new SignUpRequestDto(
                "john",
                "doe",
                "john.doe@example.com",
                "john.doe@example.com",
                "john.doe@example.com"
        );
        when(userRepository.existsByEmail(anyString())).thenReturn(false);

        // Act & Assert
        assertThrows(BadRequestException.class, () -> signupUserService.registerUser(invalidRequest),
                "Password cannot be same as email.");
        verify(userRepository).existsByEmail("john.doe@example.com");
        verifyNoMoreInteractions(userRepository);
        verifyNoInteractions(userLibrary, passwordEncoder, verificationToken, dateHelper,
                templatesService, responseBuilder, assignSubscriptionService);
    }

    @Test
    void registerUser_VerificationDetailsAndLoginAttemptsSetCorrectly() {
        // Arrange
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userLibrary.capitalizeFirstLetterOfName(anyString())).thenReturn("John");
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(verificationToken.generateVerificationToken()).thenReturn("verificationToken");
        when(dateHelper.setExpiryDate(any(OffsetDateTime.class), eq("hour"), eq(3))).thenReturn(expiryDate);
        when(dateHelper.getTimeLeftToExpiry(any(OffsetDateTime.class), any(OffsetDateTime.class), eq("hour"))).thenReturn("3");
        when(userRepository.superAdminExists()).thenReturn(false);
        when(assignSubscriptionService.assignDefaultSubscription(any(User.class))).thenReturn(new UserSubscription());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            savedUser.setId(String.valueOf(1L));
            return savedUser;
        });
        when(responseBuilder.buildUserResponse(
                any(SignupData.class), eq(HttpStatus.CREATED), eq("User registered successfully")
        )).thenReturn(any());

        // Act
        signupUserService.registerUser(request);

        // Assert
        verify(userRepository).save(argThat(u -> {
            UserVerification verification = u.getUserVerification();
            boolean verificationCorrect = verification.getEmailVerificationToken().equals("verificationToken") &&
                    verification.getStatus() == VerifiedStatus.PENDING &&
                    verification.getEmailVerificationTokenExpiry().equals(expiryDate) &&
                    !verification.isEmailVerified() &&
                    verification.getPasswordResetToken() == null &&
                    verification.getPasswordResetTokenExpiry() == null;

            LoginAttempts loginAttempts = u.getLoginAttempts();
            boolean loginAttemptsCorrect = loginAttempts != null &&
                    loginAttempts.getUser() == u;

            return verificationCorrect && loginAttemptsCorrect;
        }));
    }
}