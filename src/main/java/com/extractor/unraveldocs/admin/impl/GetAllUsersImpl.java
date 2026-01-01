package com.extractor.unraveldocs.admin.impl;

import com.extractor.unraveldocs.admin.dto.request.UserFilterDto;
import com.extractor.unraveldocs.admin.dto.response.UserListData;
import com.extractor.unraveldocs.admin.dto.response.UserSummary;
import com.extractor.unraveldocs.admin.interfaces.GetAllUsersService;
import com.extractor.unraveldocs.shared.response.ResponseBuilderService;
import com.extractor.unraveldocs.shared.response.UnravelDocsResponse;
import com.extractor.unraveldocs.user.model.User;
import com.extractor.unraveldocs.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GetAllUsersImpl implements GetAllUsersService {
        private final UserRepository userRepository;
        private final ResponseBuilderService responseBuilder;

        @Override
        @CacheEvict(value = "allUsersData", allEntries = true)
        public UnravelDocsResponse<UserListData> getAllUsers(UserFilterDto request) {
                UserListData userListData = getCachedUserListData(request);

                // Build and return the response
                return responseBuilder.buildUserResponse(
                                userListData,
                                HttpStatus.OK,
                                "Successfully fetched all users.");
        }

        @Cacheable(value = "allUsersData", key = "#request.page + '-' + #request.size + '-' + #request.sortBy + '-' + #request.sortOrder + '-' + #request.search + '-' + #request.firstName + '-' + #request.lastName + '-' + #request.email + '-' + #request.role + '-' + #request.isActive + '-' + #request.isVerified")
        public UserListData getCachedUserListData(UserFilterDto request) {
                // Create Pageable object with sorting
                Sort sort = Sort.by(Sort.Direction.fromString(request.getSortOrder()), request.getSortBy());
                Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), sort);

                // Fetch users from the repository with filtering
                Page<User> userPage = userRepository.findAllUsers(
                                request.getSearch(),
                                request.getFirstName(),
                                request.getLastName(),
                                request.getEmail(),
                                request.getRole(),
                                request.getIsActive(),
                                request.getIsVerified(),
                                pageable);

                // Convert User entities to UserSummary DTOs
                List<UserSummary> userSummaries = userPage.stream()
                                .map(this::convertToUserData)
                                .collect(Collectors.toList());

                // Set pagination details in the response
                UserListData userListData = new UserListData();
                userListData.setUsers(userSummaries);
                userListData.setTotalUsers((int) userPage.getTotalElements());
                userListData.setTotalPages(userPage.getTotalPages());
                userListData.setCurrentPage(userPage.getNumber());
                userListData.setPageSize(userPage.getSize());

                return userListData;
        }

        private UserSummary convertToUserData(User user) {
                UserSummary userSummary = new UserSummary();
                userSummary.setId(user.getId());
                userSummary.setProfilePicture(user.getProfilePicture());
                userSummary.setFirstName(user.getFirstName());
                userSummary.setLastName(user.getLastName());
                userSummary.setEmail(user.getEmail());
                userSummary.setRole(user.getRole());
                userSummary.setActive(user.isActive());
                userSummary.setVerified(user.isVerified());
                userSummary.setLastLogin(user.getLastLogin());
                userSummary.setCreatedAt(user.getCreatedAt());
                userSummary.setUpdatedAt(user.getUpdatedAt());

                return userSummary;
        }
}
