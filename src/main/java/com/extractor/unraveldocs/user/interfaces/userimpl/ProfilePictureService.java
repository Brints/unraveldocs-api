package com.extractor.unraveldocs.user.interfaces.userimpl;

import com.extractor.unraveldocs.shared.response.UnravelDocsResponse;
import com.extractor.unraveldocs.user.model.User;
import org.springframework.web.multipart.MultipartFile;

public interface ProfilePictureService {
    UnravelDocsResponse<String> uploadProfilePicture(User user, MultipartFile file);
    UnravelDocsResponse<Void> deleteProfilePicture(User user);
}
