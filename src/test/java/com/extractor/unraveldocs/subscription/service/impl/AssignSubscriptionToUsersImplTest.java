package com.extractor.unraveldocs.subscription.service.impl;

import com.extractor.unraveldocs.shared.response.ResponseBuilderService;
import com.extractor.unraveldocs.shared.response.UnravelDocsDataResponse;
import com.extractor.unraveldocs.subscription.dto.response.AllSubscriptionPlans;
import com.extractor.unraveldocs.subscription.impl.AssignSubscriptionService;
import com.extractor.unraveldocs.subscription.impl.AssignSubscriptionToUsersImpl;
import com.extractor.unraveldocs.subscription.model.UserSubscription;
import com.extractor.unraveldocs.user.model.User;
import com.extractor.unraveldocs.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AssignSubscriptionToUsersImplTest {

    @Mock
    private AssignSubscriptionService assignSubscriptionService;

    @Mock
    private ResponseBuilderService responseBuilderService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AssignSubscriptionToUsersImpl assignSubscriptionToUsersService;

    @Test
    void assignSubscriptionsToExistingUsers_success() {
        // Arrange
        User user1 = new User();
        user1.setEmail("user1@example.com");
        User user2 = new User();
        user2.setEmail("user2@example.com");
        List<User> usersWithoutSubscription = List.of(user1, user2);

        UserSubscription subscription = new UserSubscription();

        when(userRepository.findBySubscriptionIsNull()).thenReturn(usersWithoutSubscription);
        when(assignSubscriptionService.assignDefaultSubscription(any(User.class))).thenReturn(subscription);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UnravelDocsDataResponse<AllSubscriptionPlans> expectedResponse = new UnravelDocsDataResponse<>();
        when(responseBuilderService.buildUserResponse(any(AllSubscriptionPlans.class), eq(HttpStatus.OK), anyString()))
                .thenReturn(expectedResponse);

        // Act
        UnravelDocsDataResponse<AllSubscriptionPlans> actualResponse = assignSubscriptionToUsersService.assignSubscriptionsToExistingUsers();

        // Assert
        assertNotNull(actualResponse);
        assertEquals(expectedResponse, actualResponse);

        verify(userRepository, times(1)).findBySubscriptionIsNull();
        verify(assignSubscriptionService, times(2)).assignDefaultSubscription(any(User.class));
        verify(userRepository, times(2)).save(any(User.class));

        ArgumentCaptor<AllSubscriptionPlans> dataCaptor = ArgumentCaptor.forClass(AllSubscriptionPlans.class);
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(responseBuilderService).buildUserResponse(dataCaptor.capture(), eq(HttpStatus.OK), messageCaptor.capture());

        assertEquals(2, dataCaptor.getValue().getAssignedCount());
        assertEquals("Successfully assigned subscriptions to 2 users.", messageCaptor.getValue());
    }

    @Test
    void assignSubscriptionsToExistingUsers_noUsersFound() {
        // Arrange
        when(userRepository.findBySubscriptionIsNull()).thenReturn(Collections.emptyList());

        UnravelDocsDataResponse<AllSubscriptionPlans> expectedResponse = new UnravelDocsDataResponse<>();
        when(responseBuilderService.buildUserResponse(any(AllSubscriptionPlans.class), eq(HttpStatus.OK), anyString()))
                .thenReturn(expectedResponse);

        // Act
        UnravelDocsDataResponse<AllSubscriptionPlans> actualResponse = assignSubscriptionToUsersService.assignSubscriptionsToExistingUsers();

        // Assert
        assertNotNull(actualResponse);
        assertEquals(expectedResponse, actualResponse);

        verify(userRepository, times(1)).findBySubscriptionIsNull();
        verifyNoInteractions(assignSubscriptionService);
        verify(userRepository, never()).save(any());

        ArgumentCaptor<AllSubscriptionPlans> dataCaptor = ArgumentCaptor.forClass(AllSubscriptionPlans.class);
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(responseBuilderService).buildUserResponse(dataCaptor.capture(), eq(HttpStatus.OK), messageCaptor.capture());

        assertEquals(0, dataCaptor.getValue().getAssignedCount());
        assertEquals("Successfully assigned subscriptions to 0 users.", messageCaptor.getValue());
    }

    @Test
    void assignSubscriptionsToExistingUsers_handlesNullSubscriptionFromAssignService() {
        // Arrange
        User user1 = new User(); // Will get a subscription
        user1.setEmail("user1@example.com");
        User user2 = new User(); // Will fail to get a subscription
        user2.setEmail("user2@example.com");
        List<User> usersWithoutSubscription = List.of(user1, user2);

        UserSubscription validSubscription = new UserSubscription();

        when(userRepository.findBySubscriptionIsNull()).thenReturn(usersWithoutSubscription);
        when(assignSubscriptionService.assignDefaultSubscription(user1)).thenReturn(validSubscription);
        when(assignSubscriptionService.assignDefaultSubscription(user2)).thenReturn(null); // Simulate failure
        when(userRepository.save(user1)).thenReturn(user1);

        UnravelDocsDataResponse<AllSubscriptionPlans> expectedResponse = new UnravelDocsDataResponse<>();
        when(responseBuilderService.buildUserResponse(any(AllSubscriptionPlans.class), eq(HttpStatus.OK), anyString()))
                .thenReturn(expectedResponse);

        // Act
        assignSubscriptionToUsersService.assignSubscriptionsToExistingUsers();

        // Assert
        verify(userRepository, times(1)).findBySubscriptionIsNull();
        verify(assignSubscriptionService, times(2)).assignDefaultSubscription(any(User.class));
        verify(userRepository, times(1)).save(user1); // Only user1 should be saved
        verify(userRepository, never()).save(user2);

        ArgumentCaptor<AllSubscriptionPlans> dataCaptor = ArgumentCaptor.forClass(AllSubscriptionPlans.class);
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(responseBuilderService).buildUserResponse(dataCaptor.capture(), eq(HttpStatus.OK), messageCaptor.capture());

        assertEquals(1, dataCaptor.getValue().getAssignedCount());
        assertEquals("Successfully assigned subscriptions to 1 users.", messageCaptor.getValue());
    }
}