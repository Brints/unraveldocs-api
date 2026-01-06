package com.extractor.unraveldocs.user.service.impl;

import com.extractor.unraveldocs.exceptions.custom.NotFoundException;
import com.extractor.unraveldocs.user.dto.UserData;
import com.extractor.unraveldocs.user.dto.request.ProfileUpdateRequestDto;
import com.extractor.unraveldocs.shared.response.UnravelDocsResponse;
import com.extractor.unraveldocs.user.impl.ProfileUpdateImpl;
import com.extractor.unraveldocs.user.model.User;
import com.extractor.unraveldocs.user.repository.UserRepository;
import com.extractor.unraveldocs.shared.response.ResponseBuilderService;
import com.extractor.unraveldocs.elasticsearch.service.ElasticsearchIndexingService;
import com.extractor.unraveldocs.utils.userlib.UserLibrary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProfileUpdateImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private ResponseBuilderService responseBuilder;
    @Mock
    private UserLibrary userLibrary;
    @Mock
    private ElasticsearchIndexingService elasticsearchIndexingService;

    @InjectMocks
    private ProfileUpdateImpl profileUpdateImpl;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // Manually set the Optional field since @InjectMocks doesn't handle Optional<T>
        profileUpdateImpl = new ProfileUpdateImpl(
                responseBuilder,
                userLibrary,
                userRepository,
                Optional.of(elasticsearchIndexingService));
    }

    @Test
    void updateProfile_shouldUpdateFirstNameAndLastName() {
        String userId = "user123";
        User user = new User();
        user.setId(userId);
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setCountry("US");

        ProfileUpdateRequestDto request = mock(ProfileUpdateRequestDto.class);
        when(request.firstName()).thenReturn("Jane");
        when(request.lastName()).thenReturn("Smith");
        when(request.country()).thenReturn(null);
        when(request.profession()).thenReturn(null);
        when(request.organization()).thenReturn(null);

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
    void updateProfile_shouldUpdateAllFields() {
        String userId = "user123";
        User user = new User();
        user.setId(userId);
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setCountry("US");
        user.setProfession("Developer");
        user.setOrganization("OldCorp");

        ProfileUpdateRequestDto request = mock(ProfileUpdateRequestDto.class);
        when(request.firstName()).thenReturn("Jane");
        when(request.lastName()).thenReturn("Smith");
        when(request.country()).thenReturn("Canada");
        when(request.profession()).thenReturn("Engineer");
        when(request.organization()).thenReturn("NewCorp");

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
        assertEquals("Canada", user.getCountry());
        assertEquals("Engineer", user.getProfession());
        assertEquals("NewCorp", user.getOrganization());
        verify(userRepository).save(user);
    }

    @Test
    void updateProfile_shouldNotUpdateIfNoChanges() {
        String userId = "user123";
        User user = new User();
        user.setId(userId);
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setCountry("US");

        ProfileUpdateRequestDto request = mock(ProfileUpdateRequestDto.class);
        when(request.firstName()).thenReturn("John");
        when(request.lastName()).thenReturn("Doe");
        when(request.country()).thenReturn(null);
        when(request.profession()).thenReturn(null);
        when(request.organization()).thenReturn(null);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(responseBuilder.buildUserResponse(any(UserData.class), eq(HttpStatus.OK), anyString()))
                .thenReturn(new UnravelDocsResponse<>());

        UnravelDocsResponse<UserData> response = profileUpdateImpl.updateProfile(request, userId);

        assertNotNull(response);
        assertEquals("John", user.getFirstName());
        assertEquals("Doe", user.getLastName());
        // No save should occur since there are no changes
        verify(userRepository, never()).save(any(User.class));
    }
}