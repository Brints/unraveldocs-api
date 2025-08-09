package com.extractor.unraveldocs.auth.mappers;

import com.extractor.unraveldocs.auth.dto.request.SignUpRequestDto;
import com.extractor.unraveldocs.user.model.User;
import com.extractor.unraveldocs.utils.userlib.UserLibrary;
import lombok.RequiredArgsConstructor;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.security.crypto.password.PasswordEncoder;

@Mapper(componentModel = "spring")
@RequiredArgsConstructor
public abstract class UserMapper {
    protected final PasswordEncoder passwordEncoder;
    protected final UserLibrary userLibrary;

    @Mapping(target = "password", expression = "java(passwordEncoder.encode(request.password()))")
    @Mapping(target = "firstName", expression = "java(userLibrary.capitalizeFirstLetterOfName(request.firstName()))")
    @Mapping(target = "lastName", expression = "java(userLibrary.capitalizeFirstLetterOfName(request.lastName()))")
    @Mapping(target = "email", expression = "java(request.email().toLowerCase())")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "active", constant = "false")
    @Mapping(target = "verified", constant = "false")
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "lastLogin", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "profilePicture", ignore = true)
    @Mapping(target = "userVerification", ignore = true)
    @Mapping(target = "loginAttempts", ignore = true)
    @Mapping(target = "subscription", ignore = true)
    public abstract User toUser(SignUpRequestDto request);
}
