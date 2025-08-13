package com.extractor.unraveldocs.shared.response;

import com.extractor.unraveldocs.shared.request.UserDataProjection;
import com.extractor.unraveldocs.user.model.User;

import java.util.function.Supplier;

public class ResponseData {
    public static <T extends UserDataProjection> T getResponseData(User user, Supplier<T> supplier) {
        T data = supplier.get();
        data.setId(user.getId());
        data.setProfilePicture(user.getProfilePicture());
        data.setFirstName(user.getFirstName());
        data.setLastName(user.getLastName());
        data.setEmail(user.getEmail());
        data.setLastLogin(user.getLastLogin());
        data.setRole(user.getRole());
        data.setVerified(user.isVerified());
        data.setCreatedAt(user.getCreatedAt());
        data.setUpdatedAt(user.getUpdatedAt());

        return data;
    }
}
