package com.extractor.unraveldocs.auth.mappers;

import com.extractor.unraveldocs.auth.datamodel.VerifiedStatus;
import com.extractor.unraveldocs.auth.model.UserVerification;
import com.extractor.unraveldocs.user.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

import java.time.OffsetDateTime;

@Mapper(componentModel = "spring", imports = {VerifiedStatus.class})
public interface UserVerificationMapper {

    @Mappings({
            @Mapping(target = "id", ignore = true),
            @Mapping(target = "emailVerificationToken", source = "token"),
            @Mapping(target = "emailVerificationTokenExpiry", source = "expiryDate"),
            @Mapping(target = "status", expression = "java(VerifiedStatus.PENDING)"),
            @Mapping(target = "emailVerified", constant = "false"),
            @Mapping(target = "passwordResetToken", ignore = true),
            @Mapping(target = "passwordResetTokenExpiry", ignore = true),
            @Mapping(target = "user", source = "user")
    })
    UserVerification toUserVerification(User user, String token, OffsetDateTime expiryDate);
}
