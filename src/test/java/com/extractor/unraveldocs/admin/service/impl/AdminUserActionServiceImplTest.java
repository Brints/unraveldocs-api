package com.extractor.unraveldocs.admin.service.impl;

import com.extractor.unraveldocs.admin.dto.request.ActionReasonDto;
import com.extractor.unraveldocs.auth.dto.LoginData;
import com.extractor.unraveldocs.exceptions.custom.NotFoundException;
import com.extractor.unraveldocs.loginattempts.model.LoginAttempts;
import com.extractor.unraveldocs.security.JwtTokenProvider;
import com.extractor.unraveldocs.shared.response.UnravelDocsResponse;
import com.extractor.unraveldocs.user.dto.request.ForgotPasswordDto;
import com.extractor.unraveldocs.user.interfaces.userimpl.PasswordResetService;
import com.extractor.unraveldocs.user.model.User;
import com.extractor.unraveldocs.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AdminUserActionServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordResetService passwordResetService;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AdminUserActionServiceImpl testClass;

    private User sampleUser;
    private final String userId = "user-123";

    @BeforeEach
    void setUp() {
        sampleUser = new User();
        sampleUser.setId(userId);
        sampleUser.setEmail("test@example.com");
        sampleUser.setActive(true);
        sampleUser.setVerified(false);
    }

    @Test
    void toggleUserStatus_ActiveToInactive() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));
        
        ActionReasonDto reasonDto = new ActionReasonDto("Violation of terms");
        UnravelDocsResponse<String> response = testClass.toggleUserStatus(userId, false, reasonDto);

        assertFalse(sampleUser.isActive());
        verify(userRepository).save(sampleUser);
        assertNotNull(response);
    }

    @Test
    void forceVerifyEmail_AlreadyVerified() {
        sampleUser.setVerified(true);
        when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));

        UnravelDocsResponse<String> response = testClass.forceVerifyEmail(userId);

        assertTrue(sampleUser.isVerified());
        verify(userRepository, never()).save(any());
        assertEquals("User is already verified.", response.getMessage());
    }

    @Test
    void forceVerifyEmail_NotVerified() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));

        UnravelDocsResponse<String> response = testClass.forceVerifyEmail(userId);

        assertTrue(sampleUser.isVerified());
        verify(userRepository).save(sampleUser);
    }

    @Test
    void unlockUser() {
        LoginAttempts attempts = new LoginAttempts();
        attempts.setLoginAttempts(5);
        sampleUser.setLoginAttempts(attempts);
        when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));

        UnravelDocsResponse<String> response = testClass.unlockUser(userId);

        assertEquals(0, sampleUser.getLoginAttempts().getLoginAttempts());
        assertNull(sampleUser.getLoginAttempts().getBlockedUntil());
        verify(userRepository).save(sampleUser);
    }

    @Test
    void triggerPasswordReset() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));
        
        testClass.triggerPasswordReset(userId);

        ArgumentCaptor<ForgotPasswordDto> captor = ArgumentCaptor.forClass(ForgotPasswordDto.class);
        verify(passwordResetService).forgotPassword(captor.capture());
        assertEquals("test@example.com", captor.getValue().email());
    }

    @Test
    void softDeleteUser() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));
        
        ActionReasonDto reasonDto = new ActionReasonDto("Requested deletion");
        testClass.softDeleteUser(userId, reasonDto);

        assertFalse(sampleUser.isActive());
        assertNotNull(sampleUser.getDeletedAt());
        verify(userRepository).save(sampleUser);
    }

    @Test
    void impersonateUser() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));
        when(jwtTokenProvider.generateAccessToken(sampleUser)).thenReturn("access-token-123");
        when(jwtTokenProvider.generateRefreshToken(sampleUser)).thenReturn("refresh-token-123");
        when(jwtTokenProvider.getAccessExpirationInMs()).thenReturn(3600000L);

        ActionReasonDto reasonDto = new ActionReasonDto("Debugging");
        UnravelDocsResponse<LoginData> response = testClass.impersonateUser(userId, reasonDto);

        assertNotNull(response.getData());
        assertEquals("access-token-123", response.getData().accessToken());
        assertEquals(userId, response.getData().userId());
        assertEquals(3600000L, response.getData().accessExpiresIn());
    }

    @Test
    void throwsNotFoundException() {
        when(userRepository.findById(anyString())).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> testClass.forceVerifyEmail("invalid"));
    }
}
