package com.extractor.unraveldocs.user.service.impl;

import com.extractor.unraveldocs.auth.mappers.UserEventMapper;
import com.extractor.unraveldocs.auth.model.UserVerification;
import com.extractor.unraveldocs.auth.repository.UserVerificationRepository;
import com.extractor.unraveldocs.events.BaseEvent;
import com.extractor.unraveldocs.events.EventPublisherService;
import com.extractor.unraveldocs.exceptions.custom.NotFoundException;
import com.extractor.unraveldocs.user.events.UserDeletedEvent;
import com.extractor.unraveldocs.user.events.UserDeletionScheduledEvent;
import com.extractor.unraveldocs.user.impl.DeleteUserImpl;
import com.extractor.unraveldocs.user.model.User;
import com.extractor.unraveldocs.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeleteUserImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserVerificationRepository userVerificationRepository;
    @Mock
    private EventPublisherService eventPublisherService;
    @Mock
    private UserEventMapper userEventMapper;

    @InjectMocks
    private DeleteUserImpl deleteUserImpl;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId("1");
        user.setEmail("test@example.com");
        user.setFirstName("Test");
        user.setLastName("User");
        UserVerification verification = new UserVerification();
        user.setUserVerification(verification);
    }

    @Test
    void scheduleUserDeletion_shouldSetDeletedAtAndPublishEvent() {
        // Arrange
        when(userRepository.findById("1")).thenReturn(Optional.of(user));
        when(userEventMapper.toUserDeletionScheduledEvent(any(User.class), any(OffsetDateTime.class)))
                .thenReturn(new UserDeletionScheduledEvent());

        // Act
        deleteUserImpl.scheduleUserDeletion("1");

        // Assert
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertNotNull(savedUser.getDeletedAt());
        assertNotNull(savedUser.getUserVerification().getDeletedAt());
        assertEquals(savedUser.getDeletedAt(), savedUser.getUserVerification().getDeletedAt());

        verify(eventPublisherService).publishEvent(anyString(), eq("user.deletion.scheduled"), any(BaseEvent.class));
    }

    @Test
    void scheduleUserDeletion_shouldThrowIfUserNotFound() {
        // Arrange
        when(userRepository.findById("2")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(NotFoundException.class, () -> deleteUserImpl.scheduleUserDeletion("2"));
    }

    @Test
    void checkAndScheduleInactiveUsers_shouldScheduleForInactiveUsers() {
        // Arrange
        user.setActive(true);
        List<User> inactiveUsers = Collections.singletonList(user);
        Page<User> inactiveUsersPage = new PageImpl<>(inactiveUsers, PageRequest.of(0, 100), 1);

        when(userRepository.findAllByLastLoginDateBefore(any(OffsetDateTime.class), any(Pageable.class)))
                .thenReturn(inactiveUsersPage)
                .thenReturn(new PageImpl<>(Collections.emptyList())); // Stop the loop
        when(userEventMapper.toUserDeletionScheduledEvent(any(User.class), any(OffsetDateTime.class)))
                .thenReturn(new UserDeletionScheduledEvent());

        // Act
        deleteUserImpl.checkAndScheduleInactiveUsers();

        // Assert
        assertFalse(user.isActive());
        assertNotNull(user.getDeletedAt());
        assertNotNull(user.getUserVerification().getDeletedAt());
        verify(eventPublisherService).publishEvent(anyString(), eq("user.deletion.scheduled"), any(BaseEvent.class));
        verify(userRepository).saveAll(inactiveUsers);
    }

    @Test
    void processScheduledDeletions_shouldDeleteUsersAndPublishEvent() {
        // Arrange
        List<User> usersToDelete = Collections.singletonList(user);
        Page<User> usersToDeletePage = new PageImpl<>(usersToDelete, PageRequest.of(0, 100), 1);

        when(userRepository.findAllByDeletedAtBefore(any(OffsetDateTime.class), any(Pageable.class)))
                .thenReturn(usersToDeletePage)
                .thenReturn(new PageImpl<>(Collections.emptyList()));
        when(userEventMapper.toUserDeletedEvent(user)).thenReturn(new UserDeletedEvent());

        // Act
        deleteUserImpl.processScheduledDeletions();

        // Assert
        verify(eventPublisherService).publishEvent(anyString(), eq("user.deleted"), any(BaseEvent.class));
        verify(userVerificationRepository).delete(user.getUserVerification());
        verify(userRepository).deleteAll(usersToDelete);
    }

    @Test
    void deleteUser_shouldPublishEventThenDeleteUserAndRelatedData() {
        // Arrange
        when(userRepository.findById("1")).thenReturn(Optional.of(user));
        when(userEventMapper.toUserDeletedEvent(user)).thenReturn(new UserDeletedEvent());

        // Act
        deleteUserImpl.deleteUser("1");

        // Assert
        verify(eventPublisherService).publishEvent(anyString(), eq("user.deleted"), any(BaseEvent.class));
        verify(userVerificationRepository).delete(user.getUserVerification());
        verify(userRepository).delete(user);
    }

    @Test
    void deleteUser_shouldThrowIfUserNotFound() {
        // Arrange
        when(userRepository.findById("6")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(NotFoundException.class, () -> deleteUserImpl.deleteUser("6"));
    }
}