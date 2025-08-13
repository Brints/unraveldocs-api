package com.extractor.unraveldocs.admin.service.impl;

import com.extractor.unraveldocs.admin.impl.GetUserProfileByAdmin;
import com.extractor.unraveldocs.exceptions.custom.NotFoundException;
import com.extractor.unraveldocs.shared.response.ResponseBuilderService;
import com.extractor.unraveldocs.shared.response.UnravelDocsDataResponse;
import com.extractor.unraveldocs.user.dto.UserData;
import com.extractor.unraveldocs.user.model.User;
import com.extractor.unraveldocs.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class GetUserProfileByAdminTest {

    @Mock
    private ResponseBuilderService responseBuilder;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private GetUserProfileByAdmin getUserProfileByAdmin;

    @Captor
    private ArgumentCaptor<UserData> userDataCaptor;

    private User mockUser;
    private UnravelDocsDataResponse<UserData> mockResponse;
    private static final String USER_ID = "test-user-id";

    @BeforeEach
    void setUp() {
        // Create mock user
        mockUser = new User();
        mockUser.setId(USER_ID);

        // Create mock response
        mockResponse = new UnravelDocsDataResponse<>();
        mockResponse.setStatusCode(HttpStatus.OK.value());
        mockResponse.setMessage("User profile retrieved successfully");
    }

    @Test
    void shouldReturnUserProfileWhenUserExists() {
        // Arrange
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(mockUser));
        when(responseBuilder.buildUserResponse(
                any(UserData.class),
                eq(HttpStatus.OK),
                eq("User profile retrieved successfully")
        )).thenReturn(mockResponse);

        // Act
        UnravelDocsDataResponse<UserData> result = getUserProfileByAdmin.getUserProfileByAdmin(USER_ID);

        // Assert
        assertNotNull(result);
        assertEquals(mockResponse, result);

        verify(userRepository).findById(USER_ID);
        verify(responseBuilder).buildUserResponse(
                userDataCaptor.capture(),
                eq(HttpStatus.OK),
                eq("User profile retrieved successfully")
        );

        // Verify UserData was created from User
        assertNotNull(userDataCaptor.getValue());
    }

    @Test
    void shouldThrowNotFoundExceptionWhenUserDoesNotExist() {
        // Arrange
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        // Act & Assert
        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> getUserProfileByAdmin.getUserProfileByAdmin(USER_ID)
        );

        assertEquals("User not found", exception.getMessage());
        verify(userRepository).findById(USER_ID);
        verify(responseBuilder, never()).buildUserResponse(any(), any(), any());
    }
}