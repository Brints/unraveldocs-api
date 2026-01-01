package com.extractor.unraveldocs.user.impl;

import com.extractor.unraveldocs.exceptions.custom.NotFoundException;
import com.extractor.unraveldocs.shared.response.UnravelDocsResponse;
import com.extractor.unraveldocs.user.dto.UserData;
import com.extractor.unraveldocs.user.interfaces.userimpl.GetUserProfileService;
import com.extractor.unraveldocs.user.model.User;
import com.extractor.unraveldocs.user.repository.UserRepository;
import com.extractor.unraveldocs.shared.response.ResponseBuilderService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import static com.extractor.unraveldocs.shared.response.ResponseData.getResponseData;

@Service
@RequiredArgsConstructor
public class GetUserProfileImpl implements GetUserProfileService {
    private final UserRepository userRepository;
    private final ResponseBuilderService responseBuilder;

    @Override
    public UnravelDocsResponse<UserData> getUserProfileByOwner(String userId) {
        UserData data = getCachedUserData(userId);

        return responseBuilder.buildUserResponse(
                data,
                HttpStatus.OK,
                "User profile retrieved successfully");
    }

    @Cacheable(value = "userProfileData", key = "#userId")
    public UserData getCachedUserData(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        return getResponseData(user, UserData::new);
    }
}
