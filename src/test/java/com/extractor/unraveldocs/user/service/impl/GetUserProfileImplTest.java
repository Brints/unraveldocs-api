package com.extractor.unraveldocs.user.service.impl;

import com.extractor.unraveldocs.auth.datamodel.Role;
import com.extractor.unraveldocs.exceptions.custom.NotFoundException;
import com.extractor.unraveldocs.shared.response.UnravelDocsDataResponse;
import com.extractor.unraveldocs.user.dto.UserData;
import com.extractor.unraveldocs.user.impl.GetUserProfileImpl;
import com.extractor.unraveldocs.user.model.User;
import com.extractor.unraveldocs.user.repository.UserRepository;
import com.extractor.unraveldocs.shared.response.ResponseBuilderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GetUserProfileImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ResponseBuilderService responseBuilder;

    @InjectMocks
    private GetUserProfileImpl userProfile;

    private User user;
    private String userId;
    private String userEmail;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        userId = "123";
        userEmail = "test@example.com";
        user = createUser();
    }

    private User createUser() {
        User u = new User();
        u.setId(userId);
        u.setProfilePicture("pic.png");
        u.setFirstName("John");
        u.setLastName("Doe");
        u.setEmail(userEmail);
        u.setLastLogin(OffsetDateTime.now());
        u.setRole(Role.USER);
        u.setVerified(true);
        u.setCreatedAt(OffsetDateTime.now().minusDays(1));
        u.setUpdatedAt(OffsetDateTime.now());
        return u;
    }


    @Test
    void getAuthenticatedUserProfile_UserExists_ReturnsUserResponse() {
        UnravelDocsDataResponse<UserData> unravelDocsDataResponse = new UnravelDocsDataResponse<>();
        unravelDocsDataResponse.setStatusCode(HttpStatus.OK.value());
        unravelDocsDataResponse.setStatus("success");
        unravelDocsDataResponse.setMessage("User profile retrieved successfully");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(responseBuilder.buildUserResponse(any(UserData.class), eq(HttpStatus.OK), eq("User profile retrieved successfully")))
                .thenReturn(unravelDocsDataResponse);

        UnravelDocsDataResponse<UserData> result = userProfile.getUserProfileByOwner(userId);

        assertEquals(unravelDocsDataResponse, result);
        verify(userRepository).findById(userId);
    }

    @Test
    void getAuthenticatedUserProfile_UserNotFound_ThrowsNotFoundException() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> userProfile.getUserProfileByOwner(userId));
        verify(userRepository).findById(userId);
    }
}