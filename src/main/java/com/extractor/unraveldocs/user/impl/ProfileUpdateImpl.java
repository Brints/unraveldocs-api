package com.extractor.unraveldocs.user.impl;

import com.extractor.unraveldocs.exceptions.custom.NotFoundException;
import com.extractor.unraveldocs.user.dto.UserData;
import com.extractor.unraveldocs.user.dto.request.ProfileUpdateRequestDto;
import com.extractor.unraveldocs.shared.response.UnravelDocsDataResponse;
import com.extractor.unraveldocs.user.interfaces.userimpl.ProfileUpdateService;
import com.extractor.unraveldocs.user.model.User;
import com.extractor.unraveldocs.user.repository.UserRepository;
import com.extractor.unraveldocs.shared.response.ResponseBuilderService;
import com.extractor.unraveldocs.utils.imageupload.cloudinary.CloudinaryService;
import com.extractor.unraveldocs.utils.userlib.UserLibrary;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CachePut;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static com.extractor.unraveldocs.shared.response.ResponseData.getResponseData;

@Service
@RequiredArgsConstructor
public class ProfileUpdateImpl implements ProfileUpdateService {
    private final CloudinaryService cloudinaryService;
    private final ResponseBuilderService responseBuilder;
    private final UserLibrary userLibrary;
    private final UserRepository userRepository;

    @Override
    @Transactional
    @CachePut(value = {"getProfileByUser", "getProfileByAdmin"}, key = "#userId")
    public UnravelDocsDataResponse<UserData> updateProfile(ProfileUpdateRequestDto request, String userId) {
        Optional<User> optionalUser = userRepository.findById(userId);

        if (optionalUser.isEmpty()) {
            throw new NotFoundException("User not found with ID: " + userId);
        }

        User user = optionalUser.get();

        if (request.firstName() != null && !request.firstName().isEmpty() && !request.firstName().equalsIgnoreCase(user.getFirstName())) {
            String capitalizedFirstName = userLibrary.capitalizeFirstLetterOfName(request.firstName());
            user.setFirstName(capitalizedFirstName);
        }

        if (request.lastName() != null && !request.lastName().isEmpty() && !request.lastName().equalsIgnoreCase(user.getLastName())) {
            String capitalizedLastName = userLibrary.capitalizeFirstLetterOfName(request.lastName());
            user.setLastName(capitalizedLastName);
        }

        String newProfilePictureUrl;
        if (request.profilePicture() != null && !request.profilePicture().isEmpty()) {

            if (user.getProfilePicture() != null) {
                cloudinaryService.deleteFile(user.getProfilePicture());
            }

            try {
                newProfilePictureUrl = cloudinaryService.uploadFile(
                        request.profilePicture(),
                        CloudinaryService.getPROFILE_PICTURE_FOLDER(),
                        request.profilePicture().getOriginalFilename(),
                        CloudinaryService.getRESOURCE_TYPE_IMAGE());
                user.setProfilePicture(newProfilePictureUrl);
            } catch (Exception e) {
                throw new RuntimeException("Failed to upload profile picture: " + e.getMessage(), e);
            }
        }

        User updatedUser = userRepository.save(user);

        UserData data = getResponseData(updatedUser, UserData::new);

        return responseBuilder.buildUserResponse(data, HttpStatus.OK, "Profile updated successfully");
    }
}
