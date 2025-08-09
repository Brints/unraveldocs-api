package com.extractor.unraveldocs.user.service.impl;

import com.extractor.unraveldocs.auth.datamodel.VerifiedStatus;
import com.extractor.unraveldocs.auth.model.UserVerification;
import com.extractor.unraveldocs.exceptions.custom.BadRequestException;
import com.extractor.unraveldocs.exceptions.custom.ForbiddenException;
import com.extractor.unraveldocs.exceptions.custom.NotFoundException;
import com.extractor.unraveldocs.messaging.emailtemplates.UserEmailTemplateService;
import com.extractor.unraveldocs.user.dto.request.ForgotPasswordDto;
import com.extractor.unraveldocs.user.dto.request.ResetPasswordDto;
import com.extractor.unraveldocs.shared.response.UnravelDocsDataResponse;
import com.extractor.unraveldocs.user.impl.PasswordResetImpl;
import com.extractor.unraveldocs.user.interfaces.passwordreset.IPasswordReset;
import com.extractor.unraveldocs.user.model.User;
import com.extractor.unraveldocs.user.repository.UserRepository;
import com.extractor.unraveldocs.shared.response.ResponseBuilderService;
import com.extractor.unraveldocs.utils.generatetoken.GenerateVerificationToken;
import com.extractor.unraveldocs.utils.userlib.DateHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.OffsetDateTime;

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
    private UserEmailTemplateService userEmailTemplateService;

    @Mock
    private ResponseBuilderService responseBuilder;

    @InjectMocks
    private PasswordResetImpl passwordResetService;

    private User user;
    private UserVerification userVerification;
    private ForgotPasswordDto forgotPasswordDto;
    private IPasswordReset passwordResetParams;
    private ResetPasswordDto resetPasswordDto;

    @BeforeEach
    void setUp() {
        userVerification = new UserVerification();
        userVerification.setEmailVerified(true);
        userVerification.setStatus(VerifiedStatus.VERIFIED);

        user = new User();
        user.setEmail("test@example.com");
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setVerified(true);
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

        resetPasswordDto = ResetPasswordDto.builder()
                .newPassword("NewPassword123!")
                .confirmNewPassword("NewPassword123!")
                .build();
    }

    // Forgot Password Tests
    @Test
    void forgotPassword_WithValidEmail_ShouldSendResetToken() {
        // Arrange
        when(userRepository.findByEmail(anyString())).thenReturn(java.util.Optional.of(user));
        when(generateVerificationToken.generateVerificationToken()).thenReturn("generated-token");
        when(dateHelper.setExpiryDate(any(), anyString(), anyInt())).thenReturn(OffsetDateTime.now().plusHours(1));
        when(dateHelper.getTimeLeftToExpiry(any(), any(), anyString())).thenReturn("1 hour");
        when(responseBuilder.buildUserResponse(
                any(), any(), anyString()
        )).thenReturn(new UnravelDocsDataResponse<>());

        // Act
        UnravelDocsDataResponse<Void> response = passwordResetService.forgotPassword(forgotPasswordDto);

        // Assert
        assertNotNull(response);
        verify(userRepository, times(1)).save(user);
        verify(userEmailTemplateService, times(1)).sendPasswordResetToken(
                anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void forgotPassword_WithNonExistentUser_ShouldThrowNotFoundException() {
        // Arrange
        when(userRepository.findByEmail(anyString())).thenReturn(java.util.Optional.empty());

        // Act & Assert
        assertThrows(NotFoundException.class, () -> passwordResetService.forgotPassword(forgotPasswordDto));
    }

    @Test
    void forgotPassword_WithUnverifiedAccount_ShouldThrowBadRequestException() {
        // Arrange
        user.setVerified(false);
        when(userRepository.findByEmail(anyString())).thenReturn(java.util.Optional.of(user));

        // Act & Assert
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> passwordResetService.forgotPassword(forgotPasswordDto));
        assertEquals("This account is not verified. Please verify your account before resetting the password.", exception.getMessage());
    }

    @Test
    void forgotPassword_WithExistingActiveToken_ShouldThrowBadRequestException() {
        // Arrange
        userVerification.setPasswordResetToken("existing-token");
        userVerification.setPasswordResetTokenExpiry(OffsetDateTime.now().plusHours(1));
        when(userRepository.findByEmail(anyString())).thenReturn(java.util.Optional.of(user));

        // Act & Assert
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> passwordResetService.forgotPassword(forgotPasswordDto));
        assertTrue(exception.getMessage().contains("A password reset request has already been sent"));
    }

    // Reset Password Tests
    @Test
    void resetPassword_WithValidToken_ShouldResetPassword() {
        // Arrange
        userVerification.setPasswordResetToken("valid-token");
        userVerification.setPasswordResetTokenExpiry(OffsetDateTime.now().plusHours(1));
        user.setPassword("oldEncodedPassword"); // Set a password for the user
        when(userRepository.findByEmail(anyString())).thenReturn(java.util.Optional.of(user));
        when(passwordEncoder.matches(resetPasswordDto.newPassword(), user.getPassword())).thenReturn(false);
        when(passwordEncoder.encode(resetPasswordDto.newPassword())).thenReturn("encoded-password");
        when(responseBuilder.buildUserResponse(
                any(), any(), anyString()
        )).thenReturn(new UnravelDocsDataResponse<>());

        // Act
        UnravelDocsDataResponse<Void> response = passwordResetService.resetPassword(passwordResetParams, resetPasswordDto);

        // Assert
        assertNotNull(response);
        assertNull(userVerification.getPasswordResetToken());
        assertNull(userVerification.getPasswordResetTokenExpiry());
        assertEquals(VerifiedStatus.VERIFIED, userVerification.getStatus());
        verify(userRepository, times(1)).save(user);
        verify(userEmailTemplateService, times(1)).sendSuccessfulPasswordReset(
                anyString(), anyString(), anyString());
    }

    @Test
    void resetPassword_WithNonExistentUser_ShouldThrowNotFoundException() {
        // Arrange
        when(userRepository.findByEmail(anyString())).thenReturn(java.util.Optional.empty());

        // Act & Assert
        assertThrows(NotFoundException.class,
                () -> passwordResetService.resetPassword(passwordResetParams, resetPasswordDto));
    }

    @Test
    void resetPassword_WithUnverifiedAccount_ShouldThrowForbiddenException() {
        // Arrange
        user.setVerified(false);
        when(userRepository.findByEmail(anyString())).thenReturn(java.util.Optional.of(user));

        // Act & Assert
        ForbiddenException exception = assertThrows(ForbiddenException.class,
                () -> passwordResetService.resetPassword(passwordResetParams, resetPasswordDto));
        assertEquals("Account not verified. Please verify your account first.", exception.getMessage());
    }

    @Test
    void resetPassword_WithInvalidToken_ShouldThrowBadRequestException() {
        // Arrange
        userVerification.setPasswordResetToken("different-token");
        when(userRepository.findByEmail(anyString())).thenReturn(java.util.Optional.of(user));

        // Act & Assert
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> passwordResetService.resetPassword(passwordResetParams, resetPasswordDto));
        assertEquals("Invalid password reset token.", exception.getMessage());
    }

    @Test
    void resetPassword_WithExpiredToken_ShouldThrowBadRequestException() {
        // Arrange
        userVerification.setPasswordResetToken("valid-token");
        userVerification.setPasswordResetTokenExpiry(OffsetDateTime.now().minusHours(1));
        when(userRepository.findByEmail(anyString())).thenReturn(java.util.Optional.of(user));

        // Act & Assert
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> passwordResetService.resetPassword(passwordResetParams, resetPasswordDto));
        assertEquals("Password reset token has expired.", exception.getMessage());
        assertEquals(VerifiedStatus.EXPIRED, userVerification.getStatus());
        verify(userRepository, times(1)).save(user);
    }

    @Test
    void resetPassword_WithSameAsOldPassword_ShouldThrowBadRequestException() {
        // Arrange
        userVerification.setPasswordResetToken("valid-token");
        userVerification.setPasswordResetTokenExpiry(OffsetDateTime.now().plusHours(1));
        user.setPassword("oldEncodedPassword"); // Set a password for the user
        when(userRepository.findByEmail(anyString())).thenReturn(java.util.Optional.of(user));
        when(passwordEncoder.matches(resetPasswordDto.newPassword(), user.getPassword())).thenReturn(true);

        // Act & Assert
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> passwordResetService.resetPassword(passwordResetParams, resetPasswordDto));
        assertEquals("New password cannot be the same as the old password.", exception.getMessage());
    }
}