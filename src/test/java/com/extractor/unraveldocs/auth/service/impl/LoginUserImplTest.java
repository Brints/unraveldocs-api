package com.extractor.unraveldocs.auth.service.impl;

import com.extractor.unraveldocs.auth.dto.LoginData;
import com.extractor.unraveldocs.auth.dto.request.LoginRequestDto;
import com.extractor.unraveldocs.auth.datamodel.Role;
import com.extractor.unraveldocs.auth.impl.LoginUserImpl;
import com.extractor.unraveldocs.auth.model.UserVerification;
import com.extractor.unraveldocs.exceptions.custom.BadRequestException;
import com.extractor.unraveldocs.exceptions.custom.ForbiddenException;
import com.extractor.unraveldocs.exceptions.custom.TokenProcessingException;
import com.extractor.unraveldocs.shared.response.ResponseBuilderService;
import com.extractor.unraveldocs.shared.response.UnravelDocsResponse;
import com.extractor.unraveldocs.loginattempts.interfaces.LoginAttemptsService;
import com.extractor.unraveldocs.security.JwtTokenProvider;
import com.extractor.unraveldocs.security.RefreshTokenService;
import com.extractor.unraveldocs.user.model.User;
import com.extractor.unraveldocs.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;


import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class LoginUserImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private ResponseBuilderService responseBuilder;

    @Mock
    private LoginAttemptsService loginAttemptsService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private LoginUserImpl loginUserImpl;

    private User user;
    private LoginRequestDto loginRequest;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        loginRequest = new LoginRequestDto("test@example.com", "password");

        user = new User();
        user.setId("userId");
        user.setEmail("test@example.com");
        user.setPassword("encodedPassword"); // Raw password not typically stored like this, but for test object
        user.setVerified(true);
        user.setActive(true);
        user.setRole(Role.USER);
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setCreatedAt(OffsetDateTime.now().minusDays(1));
        user.setUpdatedAt(OffsetDateTime.now().minusHours(1));
        user.setLastLogin(null);


        authentication = mock(Authentication.class);
    }

    @Test
    void loginUser_successfulLogin_returnsUserLoginResponse() {
        // Arrange
        LoginData loginData = LoginData.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .role(user.getRole())
                .lastLogin(user.getLastLogin()) // Will be updated
                .isActive(user.isActive())
                .isVerified(user.isVerified())
                .accessToken("jwtAccessToken")
                .refreshToken("jwtRefreshToken")
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();

        UnravelDocsResponse<LoginData> expectedResponse = new UnravelDocsResponse<>();
        expectedResponse.setStatusCode(HttpStatus.OK.value());
        expectedResponse.setStatus("success");
        expectedResponse.setMessage("User logged in successfully");
        expectedResponse.setData(loginData);

        when(userRepository.findByEmail(loginRequest.email())).thenReturn(Optional.of(user));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(user); // Principal is the User object
        when(jwtTokenProvider.generateAccessToken(user)).thenReturn("jwtAccessToken");
        when(jwtTokenProvider.generateRefreshToken(user)).thenReturn("jwtRefreshToken");
        when(jwtTokenProvider.getJtiFromToken("jwtRefreshToken")).thenReturn("refreshTokenJti");
        // refreshTokenService.storeRefreshToken is void, no need to mock return unless specific exception

        when(responseBuilder.buildUserResponse(
                any(LoginData.class), // Use any(LoginData.class) for flexibility
                eq(HttpStatus.OK),
                eq("User logged in successfully")
        )).thenReturn(expectedResponse);

        // Act
        UnravelDocsResponse<LoginData> response = loginUserImpl.loginUser(loginRequest);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK.value(), response.getStatusCode());
        assertEquals("success", response.getStatus());
        assertEquals("User logged in successfully", response.getMessage());
        assertNotNull(response.getData());
        assertEquals("jwtAccessToken", response.getData().accessToken());
        assertEquals("jwtRefreshToken", response.getData().refreshToken());
        assertNotNull(user.getLastLogin()); // Check that lastLogin was updated on the user object

        verify(loginAttemptsService).checkIfUserBlocked(user);
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userRepository, times(1)).findByEmail(eq("test@example.com"));
        verify(loginAttemptsService).resetLoginAttempts(user);
        verify(jwtTokenProvider).generateAccessToken(user);
        verify(jwtTokenProvider).generateRefreshToken(user);
        verify(jwtTokenProvider).getJtiFromToken("jwtRefreshToken");
        verify(refreshTokenService).storeRefreshToken("refreshTokenJti", user.getId());
        verify(userRepository).save(user); // User is saved after lastLogin update
        verify(responseBuilder).buildUserResponse(any(LoginData.class), eq(HttpStatus.OK), eq("User logged in successfully"));
    }

    @Test
    void loginUser_withScheduledDeletion_cancelsDeletionAndLogsIn() {
        // Arrange
        OffsetDateTime deletionTime = OffsetDateTime.now().plusDays(5);
        user.setDeletedAt(deletionTime);
        user.setActive(false);
        UserVerification verification = new UserVerification();
        verification.setDeletedAt(deletionTime);
        user.setUserVerification(verification);

        when(userRepository.findByEmail(loginRequest.email())).thenReturn(Optional.of(user));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(user);
        when(jwtTokenProvider.generateAccessToken(user)).thenReturn("accessToken");
        when(jwtTokenProvider.generateRefreshToken(user)).thenReturn("refreshToken");
        when(jwtTokenProvider.getJtiFromToken("refreshToken")).thenReturn("jti");
        when(responseBuilder.buildUserResponse(any(LoginData.class), eq(HttpStatus.OK), anyString()))
                .thenReturn(new UnravelDocsResponse<>());

        // Act
        loginUserImpl.loginUser(loginRequest);

        // Assert
        assertNull(user.getDeletedAt(), "DeletedAt should be cleared on login.");
        assertTrue(user.isActive(), "User should be set to active on login.");
        assertNull(user.getUserVerification().getDeletedAt(), "UserVerification DeletedAt should be cleared.");
        assertNotNull(user.getLastLogin(), "Last login should be updated.");
        verify(userRepository).save(user);
    }

    @Test
    void loginUser_invalidCredentials_throwsBadRequestExceptionAndRecordsAttempt() {
        // Arrange
        when(userRepository.findByEmail(loginRequest.email())).thenReturn(Optional.of(user));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        // Act & Assert
        BadRequestException exception = assertThrows(BadRequestException.class, () -> loginUserImpl.loginUser(loginRequest));
        assertEquals("Invalid email or password", exception.getMessage());

        verify(loginAttemptsService).checkIfUserBlocked(user);
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(loginAttemptsService).recordFailedLoginAttempt(user);
        verify(userRepository, never()).save(any(User.class));
        verify(jwtTokenProvider, never()).generateAccessToken(any(User.class));
        verify(jwtTokenProvider, never()).generateRefreshToken(any(User.class));
        verify(refreshTokenService, never()).storeRefreshToken(anyString(), anyString());
        verify(responseBuilder, never()).buildUserResponse(any(), any(), anyString());
    }

    @Test
    void loginUser_userAccountDisabled_throwsBadRequestException() {
        // Arrange
        when(userRepository.findByEmail(loginRequest.email())).thenReturn(Optional.of(user));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new DisabledException("User account is disabled"));

        // Act & Assert
        BadRequestException exception = assertThrows(BadRequestException.class, () -> loginUserImpl.loginUser(loginRequest));
        assertEquals("User account is disabled. Please verify your email or contact support.", exception.getMessage());

        verify(loginAttemptsService).checkIfUserBlocked(user);
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(loginAttemptsService, never()).recordFailedLoginAttempt(any(User.class));
        verify(loginAttemptsService, never()).resetLoginAttempts(any(User.class));
        verify(userRepository, never()).save(any(User.class));
        verify(jwtTokenProvider, never()).generateAccessToken(any(User.class));
        verify(jwtTokenProvider, never()).generateRefreshToken(any(User.class));
        verify(refreshTokenService, never()).storeRefreshToken(anyString(), anyString());
    }

    @Test
    void loginUser_userAccountLocked_throwsForbiddenException() {
        // Arrange
        when(userRepository.findByEmail(loginRequest.email())).thenReturn(Optional.of(user));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new LockedException("User account is locked"));

        // Act & Assert
        ForbiddenException exception = assertThrows(ForbiddenException.class, () -> loginUserImpl.loginUser(loginRequest));
        assertEquals("User account is locked. Please contact support or try again later.", exception.getMessage());

        verify(loginAttemptsService).checkIfUserBlocked(user);
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(loginAttemptsService, never()).recordFailedLoginAttempt(any(User.class));
        verify(jwtTokenProvider, never()).generateAccessToken(any(User.class));
        verify(jwtTokenProvider, never()).generateRefreshToken(any(User.class));
        verify(refreshTokenService, never()).storeRefreshToken(anyString(), anyString());
    }

    @Test
    void loginUser_genericAuthenticationException_throwsBadRequestExceptionAndRecordsAttempt() {
        // Arrange
        when(userRepository.findByEmail(loginRequest.email())).thenReturn(Optional.of(user));
        // Use a generic AuthenticationException that is not BadCredentials, Disabled, or Locked
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new AuthenticationServiceException("Some other auth error"));

        // Act & Assert
        BadRequestException exception = assertThrows(BadRequestException.class, () -> loginUserImpl.loginUser(loginRequest));
        assertEquals("Authentication failed. Please check your credentials.", exception.getMessage());

        verify(loginAttemptsService).checkIfUserBlocked(user);
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(loginAttemptsService).recordFailedLoginAttempt(user);
        verify(jwtTokenProvider, never()).generateAccessToken(any(User.class));
        verify(jwtTokenProvider, never()).generateRefreshToken(any(User.class));
        verify(refreshTokenService, never()).storeRefreshToken(anyString(), anyString());
    }


    @Test
    void loginUser_userNotFoundByRequestEmail_throwsBadRequestException() {
        // Arrange
        when(userRepository.findByEmail(loginRequest.email())).thenReturn(Optional.empty());
        // If user not found, AuthenticationManager typically throws BadCredentialsException
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("User not found"));


        // Act & Assert
        BadRequestException exception = assertThrows(BadRequestException.class, () -> loginUserImpl.loginUser(loginRequest));
        // The specific exception thrown by authenticate() for "user not found" is BadCredentialsException,
        // which is caught and re-thrown as BadRequestException("Invalid email or password")
        assertEquals("Invalid email or password", exception.getMessage());

        verify(userRepository).findByEmail(loginRequest.email());
        // checkIfUserBlocked is NOT called because userOpt is empty
        verify(loginAttemptsService, never()).checkIfUserBlocked(any(User.class));
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        // recordFailedLoginAttempt is NOT called because userOpt is empty
        verify(loginAttemptsService, never()).recordFailedLoginAttempt(any(User.class));
        verify(responseBuilder, never()).buildUserResponse(any(), any(), anyString());
        verify(jwtTokenProvider, never()).generateAccessToken(any(User.class));
        verify(jwtTokenProvider, never()).generateRefreshToken(any(User.class));
        verify(refreshTokenService, never()).storeRefreshToken(anyString(), anyString());
    }

    @Test
    void loginUser_userIsBlocked_throwsForbiddenException() {
        // Arrange
        String blockedUserEmail = "blocked@example.com";
        LoginRequestDto blockedLoginRequest = new LoginRequestDto(blockedUserEmail, "password");
        User blockedUser = new User();
        blockedUser.setEmail(blockedUserEmail);
        blockedUser.setId("blockedUserId");

        when(userRepository.findByEmail(blockedLoginRequest.email())).thenReturn(Optional.of(blockedUser));
        String expectedMessage = "Your account is temporarily locked.";
        doThrow(new ForbiddenException(expectedMessage))
                .when(loginAttemptsService).checkIfUserBlocked(blockedUser);

        // Act & Assert
        ForbiddenException exception = assertThrows(ForbiddenException.class, () -> loginUserImpl.loginUser(blockedLoginRequest));
        assertEquals(expectedMessage, exception.getMessage());

        verify(userRepository).findByEmail(blockedLoginRequest.email());
        verify(loginAttemptsService).checkIfUserBlocked(blockedUser);
        verify(authenticationManager, never()).authenticate(any());
        verify(loginAttemptsService, never()).recordFailedLoginAttempt(any());
        verify(loginAttemptsService, never()).resetLoginAttempts(any());
        verify(jwtTokenProvider, never()).generateAccessToken(any(User.class));
        verify(jwtTokenProvider, never()).generateRefreshToken(any(User.class));
        verify(refreshTokenService, never()).storeRefreshToken(anyString(), anyString());
        verify(responseBuilder, never()).buildUserResponse(any(), any(), anyString());
    }

    @Test
    void loginUser_tokenProcessingException_whenJtiIsNull() {
        // Arrange
        when(userRepository.findByEmail(loginRequest.email())).thenReturn(Optional.of(user));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(user);
        when(jwtTokenProvider.generateAccessToken(user)).thenReturn("accessToken");
        when(jwtTokenProvider.generateRefreshToken(user)).thenReturn("refreshToken");
        when(jwtTokenProvider.getJtiFromToken("refreshToken")).thenReturn(null); // Simulate JTI generation failure

        // Act & Assert
        TokenProcessingException exception = assertThrows(TokenProcessingException.class, () -> loginUserImpl.loginUser(loginRequest));
        assertEquals("Error processing refresh token.", exception.getMessage());

        verify(loginAttemptsService).checkIfUserBlocked(user);
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(loginAttemptsService).resetLoginAttempts(user); // This is called before token generation
        verify(jwtTokenProvider).generateAccessToken(user);
        verify(jwtTokenProvider).generateRefreshToken(user);
        verify(jwtTokenProvider).getJtiFromToken("refreshToken");
        verify(refreshTokenService, never()).storeRefreshToken(anyString(), anyString());
        verify(userRepository, never()).save(user); // Not saved if token processing fails
        verify(responseBuilder, never()).buildUserResponse(any(), any(), anyString());
    }
}