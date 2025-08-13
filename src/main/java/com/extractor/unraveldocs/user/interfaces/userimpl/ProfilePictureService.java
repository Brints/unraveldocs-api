package com.extractor.unraveldocs.user.interfaces.userimpl;

import com.extractor.unraveldocs.shared.response.UnravelDocsDataResponse;
import com.extractor.unraveldocs.user.model.User;
import org.springframework.web.multipart.MultipartFile;

public interface ProfilePictureService {
    UnravelDocsDataResponse<String> uploadProfilePicture(User user, MultipartFile file);
    UnravelDocsDataResponse<Void> deleteProfilePicture(User user);
}
