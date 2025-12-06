package com.extractor.unraveldocs.admin.service.impl;

import com.extractor.unraveldocs.admin.impl.OtpCleanupServiceImpl;
import com.extractor.unraveldocs.admin.repository.OtpRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OtpCleanupServiceImplTest {

    @Mock
    private OtpRepository otpRepository;

    @InjectMocks
    private OtpCleanupServiceImpl otpCleanupService;

    @BeforeEach
    void setUp() {
        // Reset mocks before each test
        reset(otpRepository);
    }

    @Test
    void markExpiredOtps_ShouldMarkExpiredOtps_WhenExpiredOtpsExist() {
        // Arrange
        int expectedUpdatedCount = 5;
        when(otpRepository.markExpiredOtps(any(OffsetDateTime.class)))
                .thenReturn(expectedUpdatedCount);

        // Act
        otpCleanupService.markExpiredOtps();

        // Assert
        verify(otpRepository, times(1)).markExpiredOtps(any(OffsetDateTime.class));
    }

    @Test
    void markExpiredOtps_ShouldNotThrowException_WhenNoExpiredOtpsExist() {
        // Arrange
        when(otpRepository.markExpiredOtps(any(OffsetDateTime.class)))
                .thenReturn(0);

        // Act & Assert - should not throw exception
        otpCleanupService.markExpiredOtps();

        verify(otpRepository, times(1)).markExpiredOtps(any(OffsetDateTime.class));
    }

    @Test
    void markExpiredOtps_ShouldHandleExceptionGracefully() {
        // Arrange
        when(otpRepository.markExpiredOtps(any(OffsetDateTime.class)))
                .thenThrow(new RuntimeException("Database error"));

        // Act & Assert - should not propagate exception
        otpCleanupService.markExpiredOtps();

        verify(otpRepository, times(1)).markExpiredOtps(any(OffsetDateTime.class));
    }

    @Test
    void deleteOldExpiredOtps_ShouldDeleteOldOtps_WhenOldExpiredOtpsExist() {
        // Arrange
        int expectedDeletedCount = 3;
        when(otpRepository.deleteExpiredOtpsOlderThan(any(OffsetDateTime.class)))
                .thenReturn(expectedDeletedCount);

        // Act
        otpCleanupService.deleteOldExpiredOtps();

        // Assert
        verify(otpRepository, times(1)).deleteExpiredOtpsOlderThan(any(OffsetDateTime.class));
    }

    @Test
    void deleteOldExpiredOtps_ShouldNotThrowException_WhenNoOldExpiredOtpsExist() {
        // Arrange
        when(otpRepository.deleteExpiredOtpsOlderThan(any(OffsetDateTime.class)))
                .thenReturn(0);

        // Act & Assert - should not throw exception
        otpCleanupService.deleteOldExpiredOtps();

        verify(otpRepository, times(1)).deleteExpiredOtpsOlderThan(any(OffsetDateTime.class));
    }

    @Test
    void deleteOldExpiredOtps_ShouldHandleExceptionGracefully() {
        // Arrange
        when(otpRepository.deleteExpiredOtpsOlderThan(any(OffsetDateTime.class)))
                .thenThrow(new RuntimeException("Database error"));

        // Act & Assert - should not propagate exception
        otpCleanupService.deleteOldExpiredOtps();

        verify(otpRepository, times(1)).deleteExpiredOtpsOlderThan(any(OffsetDateTime.class));
    }

    @Test
    void deleteOldExpiredOtps_ShouldUse24HourThreshold() {
        // Arrange
        when(otpRepository.deleteExpiredOtpsOlderThan(any(OffsetDateTime.class)))
                .thenReturn(2);

        // Act
        otpCleanupService.deleteOldExpiredOtps();

        // Assert - verify that the threshold is approximately 24 hours ago
        verify(otpRepository, times(1)).deleteExpiredOtpsOlderThan(
                argThat(threshold -> {
                    OffsetDateTime expectedThreshold = OffsetDateTime.now().minusHours(24);
                    // Allow 1 minute tolerance for test execution time
                    return Math.abs(threshold.toEpochSecond() - expectedThreshold.toEpochSecond()) < 60;
                })
        );
    }
}

