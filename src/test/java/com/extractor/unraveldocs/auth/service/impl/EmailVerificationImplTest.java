package com.extractor.unraveldocs.auth.service.impl;

import com.extractor.unraveldocs.auth.datamodel.VerifiedStatus;
import com.extractor.unraveldocs.auth.dto.request.ResendEmailVerificationDto;
import com.extractor.unraveldocs.auth.impl.EmailVerificationImpl;
import com.extractor.unraveldocs.auth.model.UserVerification;
import com.extractor.unraveldocs.brokers.kafka.events.BaseEvent;
import com.extractor.unraveldocs.brokers.kafka.events.EventPublisherService;
import com.extractor.unraveldocs.exceptions.custom.BadRequestException;
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

        private static final String RESEND_GENERIC_MESSAGE = "If an account with this email exists and is unverified, a verification email has been sent.";

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
        void resendEmailVerification_UserNotFound_ReturnsGenericSuccess() {
                // Arrange - user not found should not throw, but return generic 200 OK (Issue
                // #3)
                UnravelDocsResponse<Void> expectedResponse = new UnravelDocsResponse<>(HttpStatus.OK.value(), "success",
                                RESEND_GENERIC_MESSAGE, null);
                when(userRepository.findByEmail("john.doe@example.com")).thenReturn(Optional.empty());
                when(responseBuilder.<Void>buildUserResponse(null, HttpStatus.OK, RESEND_GENERIC_MESSAGE))
                                .thenReturn(expectedResponse);

                // Act
                UnravelDocsResponse<Void> response = emailVerificationService.resendEmailVerification(resendRequest);

                // Assert
                assertNotNull(response);
                assertEquals(HttpStatus.OK.value(), response.getStatusCode());
                assertEquals(RESEND_GENERIC_MESSAGE, response.getMessage());
                verify(userRepository).findByEmail("john.doe@example.com");
                verifyNoInteractions(verificationToken, dateHelper, eventPublisherService);
        }

        @Test
        void resendEmailVerification_UserAlreadyVerified_ReturnsGenericSuccess() {
                // Arrange - already verified should not throw, but return generic 200 OK (Issue
                // #3)
                user.setVerified(true);
                UnravelDocsResponse<Void> expectedResponse = new UnravelDocsResponse<>(HttpStatus.OK.value(), "success",
                                RESEND_GENERIC_MESSAGE, null);
                when(userRepository.findByEmail("john.doe@example.com")).thenReturn(Optional.of(user));
                when(responseBuilder.<Void>buildUserResponse(null, HttpStatus.OK, RESEND_GENERIC_MESSAGE))
                                .thenReturn(expectedResponse);

                // Act
                UnravelDocsResponse<Void> response = emailVerificationService.resendEmailVerification(resendRequest);

                // Assert
                assertNotNull(response);
                assertEquals(HttpStatus.OK.value(), response.getStatusCode());
                assertEquals(RESEND_GENERIC_MESSAGE, response.getMessage());
                verify(userRepository).findByEmail("john.doe@example.com");
                verifyNoInteractions(verificationToken, dateHelper, eventPublisherService);
        }

        @Test
        void resendEmailVerification_ActiveTokenExists_ReturnsGenericWaitMessage() {
                // Arrange - active token should not leak TTL, return generic wait message
                // (Issue #5)
                userVerification.setEmailVerificationToken("existingToken");
                userVerification.setEmailVerificationTokenExpiry(expiryDate);
                String waitMessage = "Please wait before requesting a new verification email.";
                UnravelDocsResponse<Void> expectedResponse = new UnravelDocsResponse<>(HttpStatus.OK.value(), "success",
                                waitMessage, null);
                when(userRepository.findByEmail("john.doe@example.com")).thenReturn(Optional.of(user));
                when(responseBuilder.<Void>buildUserResponse(null, HttpStatus.OK, waitMessage))
                                .thenReturn(expectedResponse);

                // Act
                UnravelDocsResponse<Void> response = emailVerificationService.resendEmailVerification(resendRequest);

                // Assert
                assertNotNull(response);
                assertEquals(HttpStatus.OK.value(), response.getStatusCode());
                assertEquals(waitMessage, response.getMessage());
                // Verify dateHelper.getTimeLeftToExpiry is NOT called — TTL is not leaked
                verifyNoInteractions(dateHelper);
                verify(userRepository).findByEmail("john.doe@example.com");
                verifyNoInteractions(verificationToken, eventPublisherService);
        }

        @Test
        void resendEmailVerification_SuccessfulResend_ReturnsUserResponse() {
                // Arrange
                UnravelDocsResponse<Void> expectedResponse = new UnravelDocsResponse<>(HttpStatus.OK.value(), "success",
                                RESEND_GENERIC_MESSAGE, null);
                when(userRepository.findByEmail("john.doe@example.com")).thenReturn(Optional.of(user));
                when(verificationToken.generateVerificationToken()).thenReturn("newToken");
                when(dateHelper.setExpiryDate(any(OffsetDateTime.class), eq("hour"), eq(3))).thenReturn(expiryDate);
                when(dateHelper.getTimeLeftToExpiry(any(OffsetDateTime.class), eq(expiryDate), eq("hour")))
                                .thenReturn("3 hours");
                when(userRepository.save(any(User.class))).thenReturn(user);
                when(responseBuilder.<Void>buildUserResponse(null, HttpStatus.OK, RESEND_GENERIC_MESSAGE))
                                .thenReturn(expectedResponse);

                // Act
                UnravelDocsResponse<Void> response = emailVerificationService.resendEmailVerification(resendRequest);
                TransactionSynchronizationManager.getSynchronizations().get(0).afterCommit();

                // Assert
                assertNotNull(response);
                assertEquals(HttpStatus.OK.value(), response.getStatusCode());
                assertEquals("success", response.getStatus());
                assertEquals(RESEND_GENERIC_MESSAGE, response.getMessage());
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
                verify(responseBuilder).buildUserResponse(null, HttpStatus.OK, RESEND_GENERIC_MESSAGE);
                verifyNoMoreInteractions(responseBuilder);
        }

        // Tests for verifyEmail

        @Test
        void verifyEmail_UserNotFound_ThrowsBadRequestException() {
                // Arrange — Issue #3: returns BadRequestException instead of NotFoundException
                when(userRepository.findByEmail("john.doe@example.com")).thenReturn(Optional.empty());

                // Act & Assert
                BadRequestException exception = assertThrows(BadRequestException.class,
                                () -> emailVerificationService.verifyEmail("john.doe@example.com", "token"));
                assertEquals("Email verification failed.", exception.getMessage());
                assertEquals("VERIFICATION_FAILED", exception.getErrorCode());
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
                BadRequestException exception = assertThrows(BadRequestException.class,
                                () -> emailVerificationService.verifyEmail("john.doe@example.com", "token"));
                assertEquals("User is already verified. Please login.", exception.getMessage());
                assertEquals("EMAIL_ALREADY_VERIFIED", exception.getErrorCode());
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
                BadRequestException exception = assertThrows(BadRequestException.class,
                                () -> emailVerificationService.verifyEmail("john.doe@example.com", "invalidToken"));
                assertEquals("Email verification failed.", exception.getMessage());
                assertEquals("VERIFICATION_FAILED", exception.getErrorCode());
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
                BadRequestException exception = assertThrows(BadRequestException.class,
                                () -> emailVerificationService.verifyEmail("john.doe@example.com", "validToken"));
                assertEquals("Email verification token has expired.", exception.getMessage());
                assertEquals("TOKEN_EXPIRED", exception.getErrorCode());
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