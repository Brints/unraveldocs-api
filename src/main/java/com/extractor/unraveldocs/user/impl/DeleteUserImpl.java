package com.extractor.unraveldocs.user.impl;

import com.extractor.unraveldocs.auth.mappers.UserEventMapper;
import com.extractor.unraveldocs.auth.repository.UserVerificationRepository;
import com.extractor.unraveldocs.config.RabbitMQConfig;
import com.extractor.unraveldocs.events.*;
import com.extractor.unraveldocs.exceptions.custom.NotFoundException;
import com.extractor.unraveldocs.user.events.UserDeletedEvent;
import com.extractor.unraveldocs.user.events.UserDeletionScheduledEvent;
import com.extractor.unraveldocs.user.interfaces.userimpl.DeleteUserService;
import com.extractor.unraveldocs.user.model.User;
import com.extractor.unraveldocs.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeleteUserImpl implements DeleteUserService {
    private final EventPublisherService eventPublisherService;
    private final UserEventMapper userEventMapper;
    private final UserRepository userRepository;
    private final UserVerificationRepository userVerificationRepository;

    private static final int BATCH_SIZE = 100;

    @Override
    @Transactional
    public void scheduleUserDeletion(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        scheduleDeletionForUser(user);
        userRepository.save(user);
    }

    @Override
    @Transactional
    @Scheduled(cron = "0 0 0 * * ?")
    public void checkAndScheduleInactiveUsers() {
        OffsetDateTime threshold = OffsetDateTime.now().minusMonths(12);
        Pageable pageable = PageRequest.of(0, BATCH_SIZE);
        Page<User> inactiveUsersPage;

        do {
            inactiveUsersPage = userRepository.findAllByLastLoginDateBefore(threshold, pageable);
            for (User user : inactiveUsersPage.getContent()) {
                user.setActive(false);
                scheduleDeletionForUser(user);
            }
            userRepository.saveAll(inactiveUsersPage.getContent());
            pageable = inactiveUsersPage.nextPageable();
        } while (inactiveUsersPage.hasNext());
    }

    @Override
    @Transactional
    @Scheduled(cron = "0 0 1 * * ?")
    @CacheEvict(value = {"getAllUsers", "getProfileByAdmin", "getProfileByUser"}, allEntries = true)
    public void processScheduledDeletions() {
        OffsetDateTime threshold = OffsetDateTime.now();
        Pageable pageable = PageRequest.of(0, BATCH_SIZE);
        Page<User> usersToDeletePage;

        do {
            usersToDeletePage = userRepository.findAllByDeletedAtBefore(threshold, pageable);
            List<User> usersToDelete = usersToDeletePage.getContent();

            for (User user : usersToDelete) {
                publishUserDeletedEvent(user);
                if (user.getUserVerification() != null) {
                    userVerificationRepository.delete(user.getUserVerification());
                }
            }
            userRepository.deleteAll(usersToDelete);
            pageable = usersToDeletePage.nextPageable();
        } while (usersToDeletePage.hasNext());
    }

    @Override
    @Transactional
    @CacheEvict(value = {"getAllUsers", "getProfileByAdmin", "getProfileByUser"}, allEntries = true)
    public void deleteUser(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        publishUserDeletedEvent(user);

        if (user.getUserVerification() != null) {
            userVerificationRepository.delete(user.getUserVerification());
        }

        userRepository.delete(user);
    }

    private void scheduleDeletionForUser(User user) {
        OffsetDateTime deletionDate = OffsetDateTime.now().plusDays(10);
        user.setDeletedAt(deletionDate);
        if (user.getUserVerification() != null) {
            user.getUserVerification().setDeletedAt(deletionDate);
        }

        UserDeletionScheduledEvent payload = userEventMapper.toUserDeletionScheduledEvent(user, deletionDate);
        EventMetadata metadata = createEventMetadata("UserDeletionScheduled");
        BaseEvent<UserDeletionScheduledEvent> event = new BaseEvent<>(metadata, payload);

        eventPublisherService.publishEvent(
                RabbitMQConfig.USER_EVENTS_EXCHANGE,
                "user.deletion.scheduled",
                event
        );
    }

    private void publishUserDeletedEvent(User user) {
        UserDeletedEvent payload = userEventMapper.toUserDeletedEvent(user);
        EventMetadata metadata = createEventMetadata("UserDeleted");
        BaseEvent<UserDeletedEvent> event = new BaseEvent<>(metadata, payload);

        eventPublisherService.publishEvent(
                RabbitMQConfig.USER_EVENTS_EXCHANGE,
                "user.deleted",
                event
        );
    }

    private EventMetadata createEventMetadata(String eventType) {
        return EventMetadata.builder()
                .eventType(eventType)
                .eventSource("DeleteUserImpl")
                .eventTimestamp(System.currentTimeMillis())
                .correlationId(UUID.randomUUID().toString())
                .build();
    }
}