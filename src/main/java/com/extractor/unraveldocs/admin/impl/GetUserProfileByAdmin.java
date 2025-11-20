package com.extractor.unraveldocs.admin.impl;

import com.extractor.unraveldocs.admin.interfaces.GetUserProfileByAdminService;
import com.extractor.unraveldocs.exceptions.custom.NotFoundException;
import com.extractor.unraveldocs.shared.response.ResponseBuilderService;
import com.extractor.unraveldocs.shared.response.UnravelDocsResponse;
import com.extractor.unraveldocs.user.dto.UserData;
import com.extractor.unraveldocs.user.model.User;
import com.extractor.unraveldocs.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import static com.extractor.unraveldocs.shared.response.ResponseData.getResponseData;

@Service
@RequiredArgsConstructor
public class GetUserProfileByAdmin implements GetUserProfileByAdminService {
    private final ResponseBuilderService responseBuilder;
    private final UserRepository userRepository;

    @Override
    @Cacheable(value = "getProfileByAdmin", key = "#userId")
    public UnravelDocsResponse<UserData> getUserProfileByAdmin(String userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        UserData data = getResponseData(user, UserData::new);

        return responseBuilder.buildUserResponse(
                data,
                HttpStatus.OK,
                "User profile retrieved successfully"
        );
    }
}
