package com.extractor.unraveldocs.admin.service.impl;

import com.extractor.unraveldocs.admin.dto.request.UserFilterDto;
import com.extractor.unraveldocs.admin.dto.response.UserListData;
import com.extractor.unraveldocs.admin.dto.response.UserSummary;
import com.extractor.unraveldocs.admin.impl.GetAllUsersImpl;
import com.extractor.unraveldocs.auth.datamodel.Role;
import com.extractor.unraveldocs.shared.response.ResponseBuilderService;
import com.extractor.unraveldocs.shared.response.UnravelDocsDataResponse;
import com.extractor.unraveldocs.user.model.User;
import com.extractor.unraveldocs.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetAllUsersImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ResponseBuilderService responseBuilder;

    @InjectMocks
    private GetAllUsersImpl getAllUsersService;

    private UserFilterDto userFilterDto;
    private User user;
    private Page<User> userPage;
    private UnravelDocsDataResponse<UserListData> expectedResponse;

    @BeforeEach
    void setUp() {
        userFilterDto = new UserFilterDto();
        userFilterDto.setPage(0);
        userFilterDto.setSize(10);
        userFilterDto.setSortBy("createdAt");
        userFilterDto.setSortOrder("asc");
        userFilterDto.setSearch("test");
        userFilterDto.setFirstName("John");
        userFilterDto.setLastName("Doe");
        userFilterDto.setEmail("john.doe@example.com");
        userFilterDto.setRole(Role.USER);
        userFilterDto.setIsActive(true);
        userFilterDto.setIsVerified(true);

        user = new User();
        user.setId("userId1");
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setEmail("john.doe@example.com");
        user.setRole(Role.USER);
        user.setActive(true);
        user.setVerified(true);
        user.setCreatedAt(OffsetDateTime.now());
        user.setUpdatedAt(OffsetDateTime.now());
        user.setLastLogin(OffsetDateTime.now());
        user.setProfilePicture("profile.jpg");

        List<User> userList = Collections.singletonList(user);
        userPage = new PageImpl<>(userList, PageRequest.of(userFilterDto.getPage(), userFilterDto.getSize(), Sort.by(Sort.Direction.ASC, "createdAt")), 1);

        UserListData userListData = new UserListData();
        UserSummary userSummary = new UserSummary();
        userSummary.setId(user.getId());
        userSummary.setFirstName(user.getFirstName());
        userSummary.setLastName(user.getLastName());
        userSummary.setEmail(user.getEmail());
        userSummary.setRole(user.getRole());
        userSummary.setActive(user.isActive());
        userSummary.setVerified(user.isVerified());
        userSummary.setCreatedAt(user.getCreatedAt());
        userSummary.setUpdatedAt(user.getUpdatedAt());
        userSummary.setLastLogin(user.getLastLogin());
        userSummary.setProfilePicture(user.getProfilePicture());
        userListData.setUsers(Collections.singletonList(userSummary));
        userListData.setTotalUsers(1);
        userListData.setTotalPages(1);
        userListData.setCurrentPage(0);
        userListData.setPageSize(10);

        expectedResponse = new UnravelDocsDataResponse<>();
        expectedResponse.setData(userListData);
        expectedResponse.setStatus("success");
        expectedResponse.setStatusCode(HttpStatus.OK.value());
        expectedResponse.setMessage("Successfully fetched all users.");
    }

    @Test
    void getAllUsers_success() {
        Pageable pageable = PageRequest.of(
                userFilterDto.getPage(),
                userFilterDto.getSize(),
                Sort.by(Sort.Direction.fromString(userFilterDto.getSortOrder()), userFilterDto.getSortBy())
        );

        when(userRepository.findAllUsers(
                userFilterDto.getSearch(),
                userFilterDto.getFirstName(),
                userFilterDto.getLastName(),
                userFilterDto.getEmail(),
                userFilterDto.getRole(),
                userFilterDto.getIsActive(),
                userFilterDto.getIsVerified(),
                pageable
        )).thenReturn(userPage);

        when(responseBuilder.buildUserResponse(
                any(UserListData.class),
                eq(HttpStatus.OK),
                eq("Successfully fetched all users.")
        )).thenAnswer(invocation -> {
            UserListData actualListData = invocation.getArgument(0);
            // Perform detailed assertions on UserListData if necessary
            assertEquals(1, actualListData.getUsers().size());
            assertEquals(user.getId(), actualListData.getUsers().getFirst().getId());
            return expectedResponse; // Return the pre-configured expectedResponse
        });

        UnravelDocsDataResponse<UserListData> actualResponse = getAllUsersService.getAllUsers(userFilterDto);

        assertNotNull(actualResponse);
        assertEquals(expectedResponse.getStatus(), actualResponse.getStatus());
        assertEquals(expectedResponse.getMessage(), actualResponse.getMessage());
        assertNotNull(actualResponse.getData());
        assertEquals(1, actualResponse.getData().getUsers().size());
        assertEquals(user.getFirstName(), actualResponse.getData().getUsers().getFirst().getFirstName());
        assertEquals(userPage.getTotalElements(), actualResponse.getData().getTotalUsers());
        assertEquals(userPage.getTotalPages(), actualResponse.getData().getTotalPages());
        assertEquals(userPage.getNumber(), actualResponse.getData().getCurrentPage());
        assertEquals(userPage.getSize(), actualResponse.getData().getPageSize());


        verify(userRepository).findAllUsers(
                userFilterDto.getSearch(),
                userFilterDto.getFirstName(),
                userFilterDto.getLastName(),
                userFilterDto.getEmail(),
                userFilterDto.getRole(),
                userFilterDto.getIsActive(),
                userFilterDto.getIsVerified(),
                pageable
        );
        verify(responseBuilder).buildUserResponse(
                any(UserListData.class),
                eq(HttpStatus.OK),
                eq("Successfully fetched all users.")
        );
    }

    @Test
    void getAllUsers_emptyResult() {
        userFilterDto.setSearch("nonexistent");
        Page<User> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(userFilterDto.getPage(), userFilterDto.getSize(), Sort.by(Sort.Direction.ASC, "createdAt")), 0);

        Pageable pageable = PageRequest.of(
                userFilterDto.getPage(),
                userFilterDto.getSize(),
                Sort.by(Sort.Direction.fromString(userFilterDto.getSortOrder()), userFilterDto.getSortBy())
        );

        when(userRepository.findAllUsers(
                userFilterDto.getSearch(),
                userFilterDto.getFirstName(),
                userFilterDto.getLastName(),
                userFilterDto.getEmail(),
                userFilterDto.getRole(),
                userFilterDto.getIsActive(),
                userFilterDto.getIsVerified(),
                pageable
        )).thenReturn(emptyPage);

        UserListData emptyUserListData = new UserListData();
        emptyUserListData.setUsers(Collections.emptyList());
        emptyUserListData.setTotalUsers(0);
        emptyUserListData.setTotalPages(0);
        emptyUserListData.setCurrentPage(0);
        emptyUserListData.setPageSize(10);

        UnravelDocsDataResponse<UserListData> emptyExpectedResponse = new UnravelDocsDataResponse<>();
        emptyExpectedResponse.setData(emptyUserListData);
        emptyExpectedResponse.setStatus("success");
        emptyExpectedResponse.setStatusCode(HttpStatus.OK.value());
        emptyExpectedResponse.setMessage("Successfully fetched all users.");


        when(responseBuilder.buildUserResponse(
                any(UserListData.class),
                eq(HttpStatus.OK),
                eq("Successfully fetched all users.")
        )).thenReturn(emptyExpectedResponse);

        UnravelDocsDataResponse<UserListData> actualResponse = getAllUsersService.getAllUsers(userFilterDto);

        assertNotNull(actualResponse);
        assertEquals(emptyExpectedResponse, actualResponse);
        assertTrue(actualResponse.getData().getUsers().isEmpty());
        assertEquals(0, actualResponse.getData().getTotalUsers());

        verify(userRepository).findAllUsers(
                userFilterDto.getSearch(),
                userFilterDto.getFirstName(),
                userFilterDto.getLastName(),
                userFilterDto.getEmail(),
                userFilterDto.getRole(),
                userFilterDto.getIsActive(),
                userFilterDto.getIsVerified(),
                pageable
        );
        verify(responseBuilder).buildUserResponse(
                any(UserListData.class),
                eq(HttpStatus.OK),
                eq("Successfully fetched all users.")
        );
    }
}