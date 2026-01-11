package com.extractor.unraveldocs.auth.service.impl;

import com.extractor.unraveldocs.auth.datamodel.VerifiedStatus;
import com.extractor.unraveldocs.auth.dto.request.ResendEmailVerificationDto;
import com.extractor.unraveldocs.auth.impl.EmailVerificationImpl;
import com.extractor.unraveldocs.auth.model.UserVerification;
import com.extractor.unraveldocs.brokers.kafka.events.BaseEvent;
import com.extractor.unraveldocs.brokers.kafka.events.EventPublisherService;
import com.extractor.unraveldocs.exceptions.custom.BadRequestException;
import com.extractor.unraveldocs.exceptions.custom.NotFoundException;
import com.extractor.unraveldocs.shared.response.ResponseBuilderService;
import com.extractor.unraveldocs.shared.response.UnravelDocsResponse;
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
import org.springframework.transaction.support.TransactionSynchronizationManager;

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
        private ResponseBuilderService responseBuilder;

        @Mock
        private EventPublisherService eventPublisherService;

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

                TransactionSynchronizationManager.initSynchronization();
        }

        @AfterEach
        void tearDown() {
                TransactionSynchronizationManager.clear();
        }

        // Tests for resendEmailVerification
        @Test
        void resendEmailVerification_UserNotFound_ThrowsNotFoundException() {
                // Arrange
                when(userRepository.findByEmail("john.doe@example.com")).thenReturn(Optional.empty());

                // Act & Assert
                assertThrows(NotFoundException.class,
                                () -> emailVerificationService.resendEmailVerification(resendRequest),
                                "User does not exist.");
                verify(userRepository).findByEmail("john.doe@example.com");
                verifyNoMoreInteractions(userRepository);
                verifyNoInteractions(verificationToken, dateHelper, responseBuilder);
        }

        @Test
        void resendEmailVerification_UserAlreadyVerified_ThrowsBadRequestException() {
                // Arrange
                user.setVerified(true);
                when(userRepository.findByEmail("john.doe@example.com")).thenReturn(Optional.of(user));

                // Act & Assert
                assertThrows(BadRequestException.class,
                                () -> emailVerificationService.resendEmailVerification(resendRequest),
                                "User is already verified. Please login.");
                verify(userRepository).findByEmail("john.doe@example.com");
                verifyNoMoreInteractions(userRepository);
                verifyNoInteractions(verificationToken, dateHelper, responseBuilder);
        }

        @Test
        void resendEmailVerification_ActiveTokenExists_ThrowsBadRequestException() {
                // Arrange
                userVerification.setEmailVerificationToken("existingToken");
                userVerification.setEmailVerificationTokenExpiry(expiryDate);
                when(userRepository.findByEmail("john.doe@example.com")).thenReturn(Optional.of(user));
                when(dateHelper.getTimeLeftToExpiry(any(OffsetDateTime.class), eq(expiryDate), eq("hour")))
                                .thenReturn("2 hours");

                // Act & Assert
                BadRequestException exception = assertThrows(BadRequestException.class,
                                () -> emailVerificationService.resendEmailVerification(resendRequest));
                assertEquals("A verification email has already been sent. Please check your inbox or try again in 2 hours",
                                exception.getMessage());
                verify(userRepository).findByEmail("john.doe@example.com");
                verify(dateHelper).getTimeLeftToExpiry(any(OffsetDateTime.class), eq(expiryDate), eq("hour"));
                verifyNoMoreInteractions(userRepository, dateHelper);
                verifyNoInteractions(verificationToken, responseBuilder);
        }

        @Test
        void resendEmailVerification_SuccessfulResend_ReturnsUserResponse() {
                // Arrange
                UnravelDocsResponse<Void> expectedResponse = new UnravelDocsResponse<>(HttpStatus.OK.value(), "success",
                                "A new verification email has been sent successfully.", null);
                when(userRepository.findByEmail("john.doe@example.com")).thenReturn(Optional.of(user));
                when(verificationToken.generateVerificationToken()).thenReturn("newToken");
                when(dateHelper.setExpiryDate(any(OffsetDateTime.class), eq("hour"), eq(3))).thenReturn(expiryDate);
                when(dateHelper.getTimeLeftToExpiry(any(OffsetDateTime.class), eq(expiryDate), eq("hour")))
                                .thenReturn("3 hours");
                when(userRepository.save(any(User.class))).thenReturn(user);
                when(responseBuilder.<Void>buildUserResponse(
                                null, HttpStatus.OK, "A new verification email has been sent successfully."))
                                .thenReturn(expectedResponse);

                // Act
                UnravelDocsResponse<Void> response = emailVerificationService.resendEmailVerification(resendRequest);
                TransactionSynchronizationManager.getSynchronizations().get(0).afterCommit();

                // Assert
                assertNotNull(response);
                assertEquals(HttpStatus.OK.value(), response.getStatusCode());
                assertEquals("success", response.getStatus());
                assertEquals("A new verification email has been sent successfully.", response.getMessage());
                assertNull(response.getData());
                verify(userRepository).findByEmail("john.doe@example.com");
                verify(verificationToken).generateVerificationToken();
                verify(dateHelper).setExpiryDate(any(OffsetDateTime.class), eq("hour"), eq(3));
                verify(dateHelper).getTimeLeftToExpiry(any(OffsetDateTime.class), eq(expiryDate), eq("hour"));
                verify(userRepository).save(argThat(u -> {
                        UserVerification uv = u.getUserVerification();
                        return "newToken".equals(uv.getEmailVerificationToken()) &&
                                        expiryDate.equals(uv.getEmailVerificationTokenExpiry()) &&
                                        uv.getStatus() == VerifiedStatus.PENDING;
                }));
                verify(eventPublisherService).publishUserEvent(any(BaseEvent.class));
                verify(responseBuilder).buildUserResponse(
                                null, HttpStatus.OK, "A new verification email has been sent successfully.");
                verifyNoMoreInteractions(responseBuilder);
        }

        // Tests for verifyEmail
        @Test
        void verifyEmail_UserNotFound_ThrowsNotFoundException() {
                // Arrange
                when(userRepository.findByEmail("john.doe@example.com")).thenReturn(Optional.empty());

                // Act & Assert
                assertThrows(NotFoundException.class,
                                () -> emailVerificationService.verifyEmail("john.doe@example.com", "token"),
                                "User does not exist.");
                verify(userRepository).findByEmail("john.doe@example.com");
                verifyNoMoreInteractions(userRepository);
                verifyNoInteractions(verificationToken, dateHelper, responseBuilder);
        }

        @Test
        void verifyEmail_UserAlreadyVerified_ThrowsBadRequestException() {
                // Arrange
                user.setVerified(true);
                when(userRepository.findByEmail("john.doe@example.com")).thenReturn(Optional.of(user));

                // Act & Assert
                assertThrows(BadRequestException.class,
                                () -> emailVerificationService.verifyEmail("john.doe@example.com", "token"),
                                "User is already verified. Please login.");
                verify(userRepository).findByEmail("john.doe@example.com");
                verifyNoMoreInteractions(userRepository);
                verifyNoInteractions(verificationToken, dateHelper, responseBuilder);
        }

        @Test
        void verifyEmail_InvalidToken_ThrowsBadRequestException() {
                // Arrange
                userVerification.setEmailVerificationToken("validToken");
                userVerification.setEmailVerificationTokenExpiry(expiryDate);
                when(userRepository.findByEmail("john.doe@example.com")).thenReturn(Optional.of(user));

                // Act & Assert
                assertThrows(BadRequestException.class,
                                () -> emailVerificationService.verifyEmail("john.doe@example.com", "invalidToken"),
                                "Invalid email verification token.");
                verify(userRepository).findByEmail("john.doe@example.com");
                verifyNoMoreInteractions(userRepository);
                verifyNoInteractions(verificationToken, dateHelper, responseBuilder);
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
                assertThrows(BadRequestException.class,
                                () -> emailVerificationService.verifyEmail("john.doe@example.com", "validToken"),
                                "Email verification token has expired.");
                verify(userRepository).findByEmail("john.doe@example.com");
                verify(userRepository)
                                .save(argThat(u -> u.getUserVerification().getStatus() == VerifiedStatus.EXPIRED));
                verifyNoMoreInteractions(userRepository);
                verifyNoInteractions(verificationToken, dateHelper, responseBuilder);
        }

        @Test
        void verifyEmail_SuccessfulVerification_ReturnsUserResponse() {
                // Arrange
                userVerification.setEmailVerificationToken("validToken");
                userVerification.setEmailVerificationTokenExpiry(expiryDate);
                when(userRepository.findByEmail("john.doe@example.com")).thenReturn(Optional.of(user));
                when(userRepository.save(any(User.class))).thenReturn(user);

                UnravelDocsResponse<Void> expectedResponse = new UnravelDocsResponse<>(HttpStatus.OK.value(), "success",
                                "Email verified successfully", null);
                when(responseBuilder.<Void>buildUserResponse(
                                null, HttpStatus.OK, "Email verified successfully")).thenReturn(expectedResponse);

                // Act
                UnravelDocsResponse<Void> response = emailVerificationService.verifyEmail("john.doe@example.com",
                                "validToken");

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
                                null, HttpStatus.OK, "Email verified successfully");
                verifyNoMoreInteractions(userRepository, responseBuilder);
                verifyNoInteractions(verificationToken, dateHelper);
        }
}