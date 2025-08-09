package com.extractor.unraveldocs.auth.service.impl;

import com.extractor.unraveldocs.auth.dto.request.ResendEmailVerificationDto;
import com.extractor.unraveldocs.auth.datamodel.VerifiedStatus;
import com.extractor.unraveldocs.auth.impl.EmailVerificationImpl;
import com.extractor.unraveldocs.auth.model.UserVerification;
import com.extractor.unraveldocs.exceptions.custom.BadRequestException;
import com.extractor.unraveldocs.exceptions.custom.NotFoundException;
import com.extractor.unraveldocs.messaging.emailtemplates.AuthEmailTemplateService;
import com.extractor.unraveldocs.shared.response.UnravelDocsDataResponse;
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
import org.springframework.http.HttpStatus;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailVerificationImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private GenerateVerificationToken verificationToken;

    @Mock
    private DateHelper dateHelper;

    @Mock
    private AuthEmailTemplateService templatesService;

    @Mock
    private ResponseBuilderService responseBuilder;

    @InjectMocks
    private EmailVerificationImpl emailVerificationService;

    private ResendEmailVerificationDto resendRequest;
    private User user;
    private UserVerification userVerification;
    private OffsetDateTime now;
    private OffsetDateTime expiryDate;

    @BeforeEach
    void setUp() {
        now = OffsetDateTime.now();
        expiryDate = now.plusHours(3);

        resendRequest = new ResendEmailVerificationDto("john.doe@example.com");

        user = new User();
        user.setId("1");
        user.setEmail("john.doe@example.com");
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setVerified(false);
        user.setActive(false);

        userVerification = new UserVerification();
        user.setUserVerification(userVerification);
    }

    // Tests for resendEmailVerification
    @Test
    void resendEmailVerification_UserNotFound_ThrowsNotFoundException() {
        // Arrange
        when(userRepository.findByEmail("john.doe@example.com")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(NotFoundException.class, () -> emailVerificationService.resendEmailVerification(resendRequest),
                "User does not exist.");
        verify(userRepository).findByEmail("john.doe@example.com");
        verifyNoMoreInteractions(userRepository);
        verifyNoInteractions(verificationToken, dateHelper, templatesService, responseBuilder);
    }

    @Test
    void resendEmailVerification_UserAlreadyVerified_ThrowsBadRequestException() {
        // Arrange
        user.setVerified(true);
        when(userRepository.findByEmail("john.doe@example.com")).thenReturn(Optional.of(user));

        // Act & Assert
        assertThrows(BadRequestException.class, () -> emailVerificationService.resendEmailVerification(resendRequest),
                "User is already verified. Please login.");
        verify(userRepository).findByEmail("john.doe@example.com");
        verifyNoMoreInteractions(userRepository);
        verifyNoInteractions(verificationToken, dateHelper, templatesService, responseBuilder);
    }

    @Test
    void resendEmailVerification_ActiveTokenExists_ThrowsBadRequestException() {
        // Arrange
        userVerification.setEmailVerificationToken("existingToken");
        userVerification.setEmailVerificationTokenExpiry(expiryDate);
        when(userRepository.findByEmail("john.doe@example.com")).thenReturn(Optional.of(user));
        when(dateHelper.getTimeLeftToExpiry(any(OffsetDateTime.class), eq(expiryDate), eq("hour"))).thenReturn("2");

        // Act & Assert
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> emailVerificationService.resendEmailVerification(resendRequest),
                "A verification email has already been sent. Token expires in: 2");
        assertEquals("A verification email has already been sent. Token expires in: 2", exception.getMessage());
        verify(userRepository).findByEmail("john.doe@example.com");
        verify(dateHelper).getTimeLeftToExpiry(any(OffsetDateTime.class), eq(expiryDate), eq("hour"));
        verifyNoMoreInteractions(userRepository, dateHelper);
        verifyNoInteractions(verificationToken, templatesService, responseBuilder);
    }

    @Test
    void resendEmailVerification_SuccessfulResend_ReturnsUserResponse() {
        // Arrange
        when(userRepository.findByEmail("john.doe@example.com")).thenReturn(Optional.of(user));
        when(verificationToken.generateVerificationToken()).thenReturn("newToken");
        when(dateHelper.setExpiryDate(any(OffsetDateTime.class), eq("hour"), eq(3))).thenReturn(expiryDate);
        when(dateHelper.getTimeLeftToExpiry(any(OffsetDateTime.class), eq(expiryDate), eq("hour"))).thenReturn("3");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(responseBuilder.buildUserResponse(
                isNull(), eq(HttpStatus.OK), eq("Verification email sent successfully.")
        )).thenReturn(new UnravelDocsDataResponse<>(HttpStatus.OK.value(), "success", "Verification email sent successfully.", null));

        // Act
        UnravelDocsDataResponse<Void> response = emailVerificationService.resendEmailVerification(resendRequest);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK.value(), response.getStatusCode());
        assertEquals("success", response.getStatus());
        assertEquals("Verification email sent successfully.", response.getMessage());
        assertNull(response.getData());
        verify(userRepository).findByEmail("john.doe@example.com");
        verify(verificationToken).generateVerificationToken();
        verify(dateHelper).setExpiryDate(any(OffsetDateTime.class), eq("hour"), eq(3));
        verify(dateHelper).getTimeLeftToExpiry(any(OffsetDateTime.class), eq(expiryDate), eq("hour"));
        verify(userRepository).save(argThat(u -> {
            UserVerification uv = u.getUserVerification();
            return uv.getEmailVerificationToken().equals("newToken") &&
                    uv.getEmailVerificationTokenExpiry().equals(expiryDate) &&
                    uv.getStatus() == VerifiedStatus.PENDING &&
                    !uv.isEmailVerified();
        }));
        verify(templatesService).sendVerificationEmail(
                eq("john.doe@example.com"), eq("John"), eq("Doe"), eq("newToken"), eq("3"));
        verify(responseBuilder).buildUserResponse(
                isNull(), eq(HttpStatus.OK), eq("Verification email sent successfully.")
        );
        verifyNoMoreInteractions(responseBuilder);
    }

    // Tests for verifyEmail
    @Test
    void verifyEmail_UserNotFound_ThrowsNotFoundException() {
        // Arrange
        when(userRepository.findByEmail("john.doe@example.com")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(NotFoundException.class, () -> emailVerificationService.verifyEmail("john.doe@example.com", "token"),
                "User does not exist.");
        verify(userRepository).findByEmail("john.doe@example.com");
        verifyNoMoreInteractions(userRepository);
        verifyNoInteractions(verificationToken, dateHelper, templatesService, responseBuilder);
    }

    @Test
    void verifyEmail_UserAlreadyVerified_ThrowsBadRequestException() {
        // Arrange
        user.setVerified(true);
        when(userRepository.findByEmail("john.doe@example.com")).thenReturn(Optional.of(user));

        // Act & Assert
        assertThrows(BadRequestException.class, () -> emailVerificationService.verifyEmail("john.doe@example.com", "token"),
                "User is already verified. Please login.");
        verify(userRepository).findByEmail("john.doe@example.com");
        verifyNoMoreInteractions(userRepository);
        verifyNoInteractions(verificationToken, dateHelper, templatesService, responseBuilder);
    }

    @Test
    void verifyEmail_InvalidToken_ThrowsBadRequestException() {
        // Arrange
        userVerification.setEmailVerificationToken("validToken");
        userVerification.setEmailVerificationTokenExpiry(expiryDate);
        when(userRepository.findByEmail("john.doe@example.com")).thenReturn(Optional.of(user));

        // Act & Assert
        assertThrows(BadRequestException.class, () -> emailVerificationService.verifyEmail("john.doe@example.com", "invalidToken"),
                "Invalid email verification token.");
        verify(userRepository).findByEmail("john.doe@example.com");
        verifyNoMoreInteractions(userRepository);
        verifyNoInteractions(verificationToken, dateHelper, templatesService, responseBuilder);
    }

    @Test
    void verifyEmail_ExpiredToken_ThrowsBadRequestException() {
        // Arrange
        OffsetDateTime expiredDate = now.minusHours(1);
        userVerification.setEmailVerificationToken("validToken");
        userVerification.setEmailVerificationTokenExpiry(expiredDate);
        when(userRepository.findByEmail("john.doe@example.com")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        // Act & Assert
        assertThrows(BadRequestException.class, () -> emailVerificationService.verifyEmail("john.doe@example.com", "validToken"),
                "Email verification token has expired.");
        verify(userRepository).findByEmail("john.doe@example.com");
        verify(userRepository).save(argThat(u -> u.getUserVerification().getStatus() == VerifiedStatus.EXPIRED));
        verifyNoMoreInteractions(userRepository);
        verifyNoInteractions(verificationToken, dateHelper, templatesService, responseBuilder);
    }

    @Test
    void verifyEmail_SuccessfulVerification_ReturnsUserResponse() {
        // Arrange
        userVerification.setEmailVerificationToken("validToken");
        userVerification.setEmailVerificationTokenExpiry(expiryDate);
        when(userRepository.findByEmail("john.doe@example.com")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        var expectedResponse = new UnravelDocsDataResponse<>(HttpStatus.OK.value(), "success", "Email verified successfully", null);
        when(responseBuilder.buildUserResponse(
                null, HttpStatus.OK, "Email verified successfully"
        )).thenReturn(expectedResponse);

        // Act
        UnravelDocsDataResponse<Void> response = emailVerificationService.verifyEmail("john.doe@example.com", "validToken");

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK.value(), response.getStatusCode());
        assertEquals("success", response.getStatus());
        assertEquals("Email verified successfully", response.getMessage());
        assertNull(response.getData());
        verify(userRepository).findByEmail("john.doe@example.com");
        verify(userRepository).save(argThat(u -> {
            UserVerification uv = u.getUserVerification();
            return uv.getEmailVerificationToken() == null &&
                    uv.isEmailVerified() &&
                    uv.getEmailVerificationTokenExpiry() == null &&
                    uv.getStatus() == VerifiedStatus.VERIFIED &&
                    u.isVerified() &&
                    u.isActive();
        }));
        verify(responseBuilder).buildUserResponse(
                null, HttpStatus.OK, "Email verified successfully"
        );
        verifyNoMoreInteractions(userRepository, responseBuilder);
        verifyNoInteractions(verificationToken, dateHelper, templatesService);
    }
}