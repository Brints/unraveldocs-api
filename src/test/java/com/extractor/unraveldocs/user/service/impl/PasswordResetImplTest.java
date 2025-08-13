package com.extractor.unraveldocs.user.service.impl;

import com.extractor.unraveldocs.auth.datamodel.VerifiedStatus;
import com.extractor.unraveldocs.auth.mappers.UserEventMapper;
import com.extractor.unraveldocs.auth.model.UserVerification;
import com.extractor.unraveldocs.events.EventPublisherService;
import com.extractor.unraveldocs.exceptions.custom.BadRequestException;
import com.extractor.unraveldocs.exceptions.custom.ForbiddenException;
import com.extractor.unraveldocs.exceptions.custom.NotFoundException;
import com.extractor.unraveldocs.user.dto.request.ForgotPasswordDto;
import com.extractor.unraveldocs.user.dto.request.ResetPasswordDto;
import com.extractor.unraveldocs.shared.response.UnravelDocsDataResponse;
import com.extractor.unraveldocs.user.events.PasswordResetEvent;
import com.extractor.unraveldocs.user.events.PasswordResetSuccessfulEvent;
import com.extractor.unraveldocs.user.impl.PasswordResetImpl;
import com.extractor.unraveldocs.user.interfaces.passwordreset.IPasswordReset;
import com.extractor.unraveldocs.user.model.User;
import com.extractor.unraveldocs.user.repository.UserRepository;
import com.extractor.unraveldocs.shared.response.ResponseBuilderService;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private GenerateVerificationToken generateVerificationToken;
    @Mock
    private DateHelper dateHelper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private ResponseBuilderService responseBuilder;
    @Mock
    private EventPublisherService eventPublisherService;
    @Mock
    private UserEventMapper userEventMapper;

    @InjectMocks
    private PasswordResetImpl passwordResetService;

    private User user;
    private UserVerification userVerification;
    private ForgotPasswordDto forgotPasswordDto;
    private IPasswordReset passwordResetParams;
    private ResetPasswordDto resetPasswordDto;

    @BeforeEach
    void setUp() {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.initSynchronization();
        }

        user = new User();
        user.setId("user-123");
        user.setEmail("test@example.com");
        user.setFirstName("Test");
        user.setLastName("User");
        user.setVerified(true);
        user.setPassword("oldEncodedPassword"); // Set initial password

        userVerification = new UserVerification();
        userVerification.setEmailVerified(true);
        user.setUserVerification(userVerification);

        forgotPasswordDto = new ForgotPasswordDto("test@example.com");

        passwordResetParams = new IPasswordReset() {
            @Override
            public String getEmail() {
                return "test@example.com";
            }

            @Override
            public String getToken() {
                return "valid-token";
            }
        };

        resetPasswordDto = new ResetPasswordDto("newPassword123", "newPassword123");
    }

    @AfterEach
    void tearDown() {
        TransactionSynchronizationManager.clear();
    }


    // Forgot Password Tests
    @Test
    void forgotPassword_WithValidEmail_ShouldSendResetToken() {
        // Arrange
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(generateVerificationToken.generateVerificationToken()).thenReturn("reset-token");
        when(dateHelper.setExpiryDate(any(OffsetDateTime.class), eq("hour"), eq(1))).thenReturn(OffsetDateTime.now().plusHours(1));
        when(responseBuilder.buildUserResponse(isNull(), eq(HttpStatus.OK), anyString()))
                .thenReturn(new UnravelDocsDataResponse<>());
        when(userEventMapper.toPasswordResetRequestedEvent(any(), any(), any())).thenReturn(mock(PasswordResetEvent.class));

        // Act
        passwordResetService.forgotPassword(forgotPasswordDto);
        TransactionSynchronizationManager.getSynchronizations().get(0).afterCommit();

        // Assert
        verify(userRepository).save(user);
        verify(eventPublisherService).publishEvent(anyString(), anyString(), any());
    }

    @Test
    void forgotPassword_WithNonExistentUser_ShouldThrowNotFoundException() {
        // Arrange
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(NotFoundException.class, () -> passwordResetService.forgotPassword(forgotPasswordDto));
    }

    @Test
    void forgotPassword_WithUnverifiedAccount_ShouldThrowBadRequestException() {
        // Arrange
        user.setVerified(false);
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));

        // Act & Assert
        assertThrows(BadRequestException.class, () -> passwordResetService.forgotPassword(forgotPasswordDto));
    }

    @Test
    void forgotPassword_WithExistingActiveToken_ShouldThrowBadRequestException() {
        // Arrange
        userVerification.setPasswordResetToken("existing-token");
        userVerification.setPasswordResetTokenExpiry(OffsetDateTime.now().plusHours(1));
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(dateHelper.getTimeLeftToExpiry(any(), any(), anyString())).thenReturn("1 hour");

        // Act & Assert
        assertThrows(BadRequestException.class, () -> passwordResetService.forgotPassword(forgotPasswordDto));
    }

    // Reset Password Tests
    @Test
    void resetPassword_WithValidToken_ShouldResetPassword() {
        // Arrange
        userVerification.setPasswordResetToken("valid-token");
        userVerification.setPasswordResetTokenExpiry(OffsetDateTime.now().plusHours(1));
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(resetPasswordDto.newPassword(), user.getPassword())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedNewPassword");
        when(responseBuilder.buildUserResponse(isNull(), eq(HttpStatus.OK), anyString()))
                .thenReturn(new UnravelDocsDataResponse<>());
        when(userEventMapper.toPasswordResetSuccessfulEvent(any())).thenReturn(mock(PasswordResetSuccessfulEvent.class));

        // Act
        passwordResetService.resetPassword(passwordResetParams, resetPasswordDto);
        TransactionSynchronizationManager.getSynchronizations().get(0).afterCommit();

        // Assert
        verify(userRepository).save(user);
        assertNull(userVerification.getPasswordResetToken());
        assertNull(userVerification.getPasswordResetTokenExpiry());
        assertEquals(VerifiedStatus.VERIFIED, userVerification.getStatus());
        verify(eventPublisherService).publishEvent(anyString(), anyString(), any());
    }

    @Test
    void resetPassword_WithNonExistentUser_ShouldThrowNotFoundException() {
        // Arrange
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(NotFoundException.class, () -> passwordResetService.resetPassword(passwordResetParams, resetPasswordDto));
    }

    @Test
    void resetPassword_WithUnverifiedAccount_ShouldThrowForbiddenException() {
        // Arrange
        user.setVerified(false);
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));

        // Act & Assert
        assertThrows(ForbiddenException.class, () -> passwordResetService.resetPassword(passwordResetParams, resetPasswordDto));
    }

    @Test
    void resetPassword_WithInvalidToken_ShouldThrowBadRequestException() {
        // Arrange
        userVerification.setPasswordResetToken("invalid-token");
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));

        // Act & Assert
        assertThrows(BadRequestException.class, () -> passwordResetService.resetPassword(passwordResetParams, resetPasswordDto));
    }

    @Test
    void resetPassword_WithExpiredToken_ShouldThrowBadRequestException() {
        // Arrange
        userVerification.setPasswordResetToken("valid-token");
        userVerification.setPasswordResetTokenExpiry(OffsetDateTime.now().minusHours(1));
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));

        // Act & Assert
        assertThrows(BadRequestException.class, () -> passwordResetService.resetPassword(passwordResetParams, resetPasswordDto));
        assertEquals(VerifiedStatus.EXPIRED, userVerification.getStatus());
        verify(userRepository).save(user);
    }

    @Test
    void resetPassword_WithSameAsOldPassword_ShouldThrowBadRequestException() {
        // Arrange
        userVerification.setPasswordResetToken("valid-token");
        userVerification.setPasswordResetTokenExpiry(OffsetDateTime.now().plusHours(1));
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(resetPasswordDto.newPassword(), user.getPassword())).thenReturn(true);

        // Act & Assert
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> passwordResetService.resetPassword(passwordResetParams, resetPasswordDto));
        assertEquals("New password cannot be the same as the old password.", exception.getMessage());
    }
}