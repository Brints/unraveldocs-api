package com.extractor.unraveldocs.user.impl;

import com.extractor.unraveldocs.exceptions.custom.BadRequestException;
import com.extractor.unraveldocs.shared.response.ResponseBuilderService;
import com.extractor.unraveldocs.shared.response.UnravelDocsDataResponse;
import com.extractor.unraveldocs.user.interfaces.userimpl.ProfilePictureService;
import com.extractor.unraveldocs.user.model.User;
import com.extractor.unraveldocs.user.repository.UserRepository;
import com.extractor.unraveldocs.utils.imageupload.aws.AwsS3Service;
import com.extractor.unraveldocs.utils.imageupload.FileType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ProfilePictureImpl implements ProfilePictureService {
    private final AwsS3Service awsS3Service;
    private final ResponseBuilderService builderService;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public UnravelDocsDataResponse<String> uploadProfilePicture(User user, MultipartFile file) {
        if (FileType.IMAGE.isValid(file.getContentType())) {
            throw new BadRequestException("Invalid file type. Only image files are allowed.");
        }

        String fileName = awsS3Service.generateFileName(file.getOriginalFilename(), AwsS3Service.getPROFILE_PICTURE_FOLDER());
        String profilePictureUrl = awsS3Service.uploadFile(file, fileName);

        user.setProfilePicture(profilePictureUrl);
        userRepository.save(user);

        return builderService
                .buildUserResponse(
                        profilePictureUrl,
                        HttpStatus.OK,
                        "Profile picture uploaded successfully."
                );
    }

    @Override
    @Transactional
    public UnravelDocsDataResponse<Void> deleteProfilePicture(User user) {
        if (user.getProfilePicture() == null || user.getProfilePicture().isEmpty()) {
            return builderService.buildUserResponse(
                    null,
                    HttpStatus.BAD_REQUEST,
                    "No profile picture to delete."
            );
        }
        awsS3Service.deleteFile(user.getProfilePicture());

        user.setProfilePicture(null);
        userRepository.save(user);

        return builderService.buildUserResponse(
                        null,
                        HttpStatus.OK,
                        "Profile picture deleted successfully."
                );
    }
}
