package com.extractor.unraveldocs.auth.mappers;

import com.extractor.unraveldocs.auth.events.UserRegisteredEvent;
import com.extractor.unraveldocs.user.events.*;
import com.extractor.unraveldocs.user.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

import java.time.OffsetDateTime;

@Mapper(componentModel = "spring")
public interface UserEventMapper {
    @Mappings({
            @Mapping(target = "email", source = "user.email"),
            @Mapping(target = "firstName", source = "user.firstName"),
            @Mapping(target = "lastName", source = "user.lastName"),
            @Mapping(target = "verificationToken", source = "token"),
            @Mapping(target = "expiration", source = "expiration")
    })
    UserRegisteredEvent toUserRegisteredEvent(User user, String token, String expiration);

    @Mapping(target = "deletionDate", source = "deletionDate")
    UserDeletionScheduledEvent toUserDeletionScheduledEvent(User user, OffsetDateTime deletionDate);

    @Mapping(target = "profilePictureUrl", source = "profilePicture")
    UserDeletedEvent toUserDeletedEvent(User user);

    @Mappings({
            @Mapping(target = "email", source = "user.email"),
            @Mapping(target = "firstName", source = "user.firstName"),
            @Mapping(target = "lastName", source = "user.lastName")
    })
    PasswordChangedEvent toPasswordChangedEvent(User user);

    @Mappings({
            @Mapping(target = "email", source = "user.email"),
            @Mapping(target = "firstName", source = "user.firstName"),
            @Mapping(target = "lastName", source = "user.lastName"),
            @Mapping(target = "token", source = "token"),
            @Mapping(target = "expiration", source = "expiration")
    })
    PasswordResetRequestedEvent toPasswordResetRequestedEvent(User user, String token, String expiration);

    @Mappings({
            @Mapping(target = "email", source = "user.email"),
            @Mapping(target = "firstName", source = "user.firstName"),
            @Mapping(target = "lastName", source = "user.lastName")
    })
    PasswordResetSuccessfulEvent toPasswordResetSuccessfulEvent(User user);
}