package com.extractor.unraveldocs.user.service.impl;

import com.extractor.unraveldocs.exceptions.custom.NotFoundException;
import com.extractor.unraveldocs.user.dto.UserData;
import com.extractor.unraveldocs.user.dto.request.ProfileUpdateRequestDto;
import com.extractor.unraveldocs.shared.response.UnravelDocsResponse;
import com.extractor.unraveldocs.user.impl.ProfileUpdateImpl;
import com.extractor.unraveldocs.user.model.User;
import com.extractor.unraveldocs.user.repository.UserRepository;
import com.extractor.unraveldocs.shared.response.ResponseBuilderService;
import com.extractor.unraveldocs.utils.imageupload.cloudinary.CloudinaryService;
import com.extractor.unraveldocs.utils.userlib.UserLibrary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProfileUpdateImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private CloudinaryService cloudinaryService;
    @Mock
    private ResponseBuilderService responseBuilder;
    @Mock
    private UserLibrary userLibrary;

    @InjectMocks
    private ProfileUpdateImpl profileUpdateImpl;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void updateProfile_shouldUpdateFirstNameAndLastName() {
        String userId = "user123";
        User user = new User();
        user.setId(userId);
        user.setFirstName("John");
        user.setLastName("Doe");

        ProfileUpdateRequestDto request = mock(ProfileUpdateRequestDto.class);
        when(request.firstName()).thenReturn("Jane");
        when(request.lastName()).thenReturn("Smith");
        when(request.profilePicture()).thenReturn(null);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userLibrary.capitalizeFirstLetterOfName("Jane")).thenReturn("Jane");
        when(userLibrary.capitalizeFirstLetterOfName("Smith")).thenReturn("Smith");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        when(responseBuilder.buildUserResponse(any(UserData.class), eq(HttpStatus.OK), anyString()))
                .thenReturn(new UnravelDocsResponse<>());

        UnravelDocsResponse<UserData> response = profileUpdateImpl.updateProfile(request, userId);

        assertNotNull(response);
        assertEquals("Jane", user.getFirstName());
        assertEquals("Smith", user.getLastName());
        verify(userRepository).save(user);
    }

    @Test
    void updateProfile_shouldThrowNotFoundExceptionIfUserNotFound() {
        String userId = "user123";
        ProfileUpdateRequestDto request = mock(ProfileUpdateRequestDto.class);

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> profileUpdateImpl.updateProfile(request, userId));
    }

    @Test
    void updateProfile_shouldUpdateProfilePicture() throws Exception {
        String userId = "user123";
        User user = new User();
        user.setId(userId);
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setProfilePicture("old-url");

        MultipartFile mockFile = mock(MultipartFile.class);
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getOriginalFilename()).thenReturn("profile.jpg");

        ProfileUpdateRequestDto request = mock(ProfileUpdateRequestDto.class);
        when(request.firstName()).thenReturn("John");
        when(request.lastName()).thenReturn("Doe");
        when(request.profilePicture()).thenReturn(mockFile);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        doNothing().when(cloudinaryService).deleteFile("old-url");
        when(cloudinaryService.uploadFile(
                eq(mockFile),
                anyString(),
                eq("profile.jpg"),
                anyString()
        )).thenReturn("new-url");

        when(userLibrary.capitalizeFirstLetterOfName("John")).thenReturn("John");
        when(userLibrary.capitalizeFirstLetterOfName("Doe")).thenReturn("Doe");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        when(responseBuilder.buildUserResponse(any(UserData.class), eq(HttpStatus.OK), anyString()))
                .thenReturn(new UnravelDocsResponse<>());

        UnravelDocsResponse<UserData> response = profileUpdateImpl.updateProfile(request, userId);

        assertNotNull(response);
        assertEquals("new-url", user.getProfilePicture());
        verify(cloudinaryService).deleteFile("old-url");
        verify(cloudinaryService).uploadFile(
                eq(mockFile),
                anyString(),
                eq("profile.jpg"),
                anyString()
        );
    }

    @Test
    void updateProfile_shouldNotUpdateIfNoChanges() {
        String userId = "user123";
        User user = new User();
        user.setId(userId);
        user.setFirstName("John");
        user.setLastName("Doe");

        ProfileUpdateRequestDto request = mock(ProfileUpdateRequestDto.class);
        when(request.firstName()).thenReturn("John");
        when(request.lastName()).thenReturn("Doe");
        when(request.profilePicture()).thenReturn(null);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        when(responseBuilder.buildUserResponse(any(UserData.class), eq(HttpStatus.OK), anyString()))
                .thenReturn(new UnravelDocsResponse<>());

        UnravelDocsResponse<UserData> response = profileUpdateImpl.updateProfile(request, userId);

        assertNotNull(response);
        assertEquals("John", user.getFirstName());
        assertEquals("Doe", user.getLastName());
        verify(userRepository).save(user);
    }
}