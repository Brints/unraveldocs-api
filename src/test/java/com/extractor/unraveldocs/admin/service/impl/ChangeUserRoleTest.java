package com.extractor.unraveldocs.admin.service.impl;

import com.extractor.unraveldocs.admin.dto.AdminData;
import com.extractor.unraveldocs.admin.dto.request.ChangeRoleDto;
import com.extractor.unraveldocs.admin.impl.ChangeUserRoleImpl;
import com.extractor.unraveldocs.auth.datamodel.Role;
import com.extractor.unraveldocs.exceptions.custom.ForbiddenException;
import com.extractor.unraveldocs.exceptions.custom.NotFoundException;
import com.extractor.unraveldocs.exceptions.custom.UnauthorizedException;
import com.extractor.unraveldocs.shared.response.ResponseBuilderService;
import com.extractor.unraveldocs.shared.response.UnravelDocsResponse;
import com.extractor.unraveldocs.subscription.impl.AssignSubscriptionService;
import com.extractor.unraveldocs.subscription.model.UserSubscription;
import com.extractor.unraveldocs.user.model.User;
import com.extractor.unraveldocs.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ChangeUserRoleTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ResponseBuilderService responseBuilder;

    @Mock
    private Authentication authentication;

    @Mock
    private AssignSubscriptionService assignSubscriptionService;

    @InjectMocks
    private ChangeUserRoleImpl changeUserRoleService;

    private ChangeRoleDto changeRoleDto;
    private User user;

    @BeforeEach
    void setUp() {
        changeRoleDto = new ChangeRoleDto();
        changeRoleDto.setUserId("testUserId");
        changeRoleDto.setRole(Role.USER);

        user = new User();
        user.setId("testUserId");
        user.setVerified(true);
        user.setRole(Role.ADMIN);
    }

    private void mockAdminAuthentication() {
        Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_admin"));
        when(authentication.getAuthorities()).thenReturn((Collection) authorities);
    }

    private void mockSuperAdminAuthentication() {
        Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_super_admin"));
        when(authentication.getAuthorities()).thenReturn((Collection) authorities);
    }

    private void mockUserAuthentication() {
        Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_user"));
        when(authentication.getAuthorities()).thenReturn((Collection) authorities);
    }

    @Test
    void changeUserRole_success_asAdmin() {
        mockAdminAuthentication();
        when(userRepository.findById("testUserId")).thenReturn(Optional.of(user));
        when(assignSubscriptionService.assignDefaultSubscription(any(User.class))).thenReturn(new UserSubscription());
        when(userRepository.save(any(User.class))).thenReturn(user);

        UnravelDocsResponse<AdminData> expectedResponse = new UnravelDocsResponse<>();
        when(responseBuilder.buildUserResponse(any(AdminData.class), eq(HttpStatus.OK), eq("User role changed successfully.")))
                .thenReturn(expectedResponse);

        UnravelDocsResponse<AdminData> actualResponse = changeUserRoleService.changeUserRole(changeRoleDto, authentication);

        assertNotNull(actualResponse);
        assertEquals(expectedResponse, actualResponse);
        assertEquals(Role.USER, user.getRole());
        verify(userRepository, times(1)).findById("testUserId");
        verify(assignSubscriptionService, times(1)).assignDefaultSubscription(user);
        verify(userRepository, times(1)).save(user);
        verify(responseBuilder, times(1)).buildUserResponse(any(AdminData.class), eq(HttpStatus.OK), eq("User role changed successfully."));
    }

    @Test
    void changeUserRole_success_asSuperAdmin() {
        mockSuperAdminAuthentication();
        when(userRepository.findById("testUserId")).thenReturn(Optional.of(user));
        when(assignSubscriptionService.assignDefaultSubscription(any(User.class))).thenReturn(new UserSubscription());
        when(userRepository.save(any(User.class))).thenReturn(user);

        UnravelDocsResponse<AdminData> expectedResponse = new UnravelDocsResponse<>();
        when(responseBuilder.buildUserResponse(any(AdminData.class), eq(HttpStatus.OK), eq("User role changed successfully.")))
                .thenReturn(expectedResponse);

        UnravelDocsResponse<AdminData> actualResponse = changeUserRoleService.changeUserRole(changeRoleDto, authentication);

        assertNotNull(actualResponse);
        assertEquals(expectedResponse, actualResponse);
        assertEquals(Role.USER, user.getRole());
        verify(userRepository, times(1)).findById("testUserId");
        verify(assignSubscriptionService, times(1)).assignDefaultSubscription(user);
        verify(userRepository, times(1)).save(user);
        verify(responseBuilder, times(1)).buildUserResponse(any(AdminData.class), eq(HttpStatus.OK), eq("User role changed successfully."));
    }


    @Test
    void changeUserRole_throwsUnauthorizedException_whenNotAdminOrSuperAdmin() {
        mockUserAuthentication(); // Non-admin role

        UnauthorizedException exception = assertThrows(UnauthorizedException.class, () -> {
            changeUserRoleService.changeUserRole(changeRoleDto, authentication);
        });

        assertEquals("You must be an admin or super admin to change user roles", exception.getMessage());
        verify(userRepository, never()).findById(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void changeUserRole_throwsNotFoundException_whenUserNotFound() {
        mockAdminAuthentication();
        when(userRepository.findById("testUserId")).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class, () -> {
            changeUserRoleService.changeUserRole(changeRoleDto, authentication);
        });

        assertEquals("User not found with ID: testUserId", exception.getMessage());
        verify(userRepository, times(1)).findById("testUserId");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void changeUserRole_throwsForbiddenException_whenUserNotVerified() {
        mockAdminAuthentication();
        user.setVerified(false); // User is not verified
        when(userRepository.findById("testUserId")).thenReturn(Optional.of(user));

        ForbiddenException exception = assertThrows(ForbiddenException.class, () -> {
            changeUserRoleService.changeUserRole(changeRoleDto, authentication);
        });

        assertEquals("Cannot change role for unverified users", exception.getMessage());
        verify(userRepository, times(1)).findById("testUserId");
        verify(userRepository, never()).save(any(User.class));
    }
}