package com.extractor.unraveldocs.storage.service;

import com.extractor.unraveldocs.documents.repository.DocumentCollectionRepository;
import com.extractor.unraveldocs.documents.utils.SanitizeLogging;
import com.extractor.unraveldocs.storage.dto.StorageInfo;
import com.extractor.unraveldocs.storage.exception.StorageQuotaExceededException;
import com.extractor.unraveldocs.subscription.model.SubscriptionPlan;
import com.extractor.unraveldocs.subscription.model.UserSubscription;
import com.extractor.unraveldocs.subscription.repository.UserSubscriptionRepository;
import com.extractor.unraveldocs.team.model.Team;
import com.extractor.unraveldocs.team.model.TeamMember;
import com.extractor.unraveldocs.team.model.TeamSubscriptionPlan;
import com.extractor.unraveldocs.team.repository.TeamMemberRepository;
import com.extractor.unraveldocs.team.repository.TeamRepository;
import com.extractor.unraveldocs.user.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing storage allocation and usage tracking.
 * Handles both individual user and team storage.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StorageAllocationService {

    private final UserSubscriptionRepository userSubscriptionRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final DocumentCollectionRepository documentCollectionRepository;
    private final SanitizeLogging sanitizer;

    /**
     * Check if user has sufficient storage available for upload.
     * Checks team storage first if user is part of a team, otherwise checks
     * individual storage.
     *
     * @param user          The user attempting the upload
     * @param requiredBytes The number of bytes required for the upload
     * @throws StorageQuotaExceededException if storage limit would be exceeded
     */
    public void checkStorageAvailable(User user, long requiredBytes) {
        // Check if user is part of a team first
        List<TeamMember> teamMemberships = teamMemberRepository.findByUserId(user.getId());
        Optional<TeamMember> teamMembership = teamMemberships.stream().findFirst();

        if (teamMembership.isPresent()) {
            Team team = teamMembership.get().getTeam();
            if (team.isAccessAllowed()) {
                checkTeamStorageAvailable(team, requiredBytes);
                return;
            }
        }

        checkIndividualStorageAvailable(user, requiredBytes);
        checkIndividualStorageAvailable(user, requiredBytes);
    }

    /**
     * Check if user has sufficient document upload slots available.
     *
     * @param user              The user attempting the upload
     * @param newDocumentsCount The number of new documents to upload
     * @throws StorageQuotaExceededException if document limit would be exceeded
     */
    public void checkDocumentUploadLimit(User user, int newDocumentsCount) {
        // Check if user is part of a team first
        List<TeamMember> teamMemberships = teamMemberRepository.findByUserId(user.getId());
        Optional<TeamMember> teamMembership = teamMemberships.stream().findFirst();

        if (teamMembership.isPresent()) {
            Team team = teamMembership.get().getTeam();
            if (team.isAccessAllowed()) {
                checkTeamDocumentUploadLimit(team, user, newDocumentsCount);
                return;
            }
        }

        checkIndividualDocumentUploadLimit(user, newDocumentsCount);
    }

    private void checkIndividualDocumentUploadLimit(User user, int newDocumentsCount) {
        Optional<UserSubscription> subscriptionOpt = userSubscriptionRepository.findByUserIdWithPlan(user.getId());
        if (subscriptionOpt.isEmpty()) {
            throw new StorageQuotaExceededException("No active subscription found.");
        }

        UserSubscription subscription = subscriptionOpt.get();
        SubscriptionPlan plan = subscription.getPlan();
        Integer documentUploadLimit = plan.getDocumentUploadLimit();

        if (documentUploadLimit == null || documentUploadLimit == 0) {
            return; // Unlimited
        }

        // Use monthly documents uploaded count (resets monthly) instead of total document count
        int currentMonthlyCount = subscription.getMonthlyDocumentsUploaded() != null
                ? subscription.getMonthlyDocumentsUploaded() : 0;
        // Use long arithmetic to prevent integer overflow
        long totalDocuments = (long) currentMonthlyCount + (long) newDocumentsCount;
        if (totalDocuments > documentUploadLimit) {
            throw new StorageQuotaExceededException(
                    "Monthly document upload limit exceeded. Limit: " + documentUploadLimit +
                    ", Used this month: " + currentMonthlyCount + ", Attempting to add: " + newDocumentsCount +
                    ". Your quota will reset on the first day of next month.");
        }
    }

    private void checkTeamDocumentUploadLimit(Team team, User user, int newDocumentsCount) {
        TeamSubscriptionPlan plan = team.getPlan();
        if (plan == null)
            return;

        Integer documentUploadLimit = plan.getMonthlyDocumentLimit();

        if (documentUploadLimit == null) {
            return; // Unlimited (Enterprise)
        }

        long currentCount = documentCollectionRepository.countByUserId(user.getId());

        if (currentCount + newDocumentsCount > documentUploadLimit) {
            throw new StorageQuotaExceededException(
                    "Team document upload limit exceeded. Limit: " + documentUploadLimit + ", Current: " + currentCount
                            + ", New: " + newDocumentsCount);
        }
    }

    /**
     * Check if individual user has sufficient storage available.
     */
    private void checkIndividualStorageAvailable(User user, long requiredBytes) {
        Optional<UserSubscription> subscriptionOpt = userSubscriptionRepository.findByUserIdWithPlan(user.getId());

        if (subscriptionOpt.isEmpty()) {
            throw new StorageQuotaExceededException("No active subscription found. Please subscribe to a plan.");
        }

        UserSubscription subscription = subscriptionOpt.get();

        SubscriptionPlan plan = subscription.getPlan();
        Long storageLimit = plan.getStorageLimit();
        Long storageUsed = subscription.getStorageUsed();

        // If storage limit is null, treat as unlimited (shouldn't happen for individual
        // plans)
        if (storageLimit == null) {
            return;
        }

        long availableStorage = storageLimit - storageUsed;

        if (requiredBytes > availableStorage) {
            throw new StorageQuotaExceededException(requiredBytes, availableStorage, storageLimit);
        }
    }

    /**
     * Check if team has sufficient storage available.
     *
     * @param team          The team to check
     * @param requiredBytes The number of bytes required
     * @throws StorageQuotaExceededException if team storage limit would be exceeded
     */
    public void checkTeamStorageAvailable(Team team, long requiredBytes) {
        Long storageLimit = team.getPlan() != null ? team.getPlan().getStorageLimit() : null;
        Long storageUsed = team.getStorageUsed();

        // If storage limit is null, team has unlimited storage (Enterprise)
        if (storageLimit == null) {
            return;
        }

        long availableStorage = storageLimit - storageUsed;

        if (requiredBytes > availableStorage) {
            throw new StorageQuotaExceededException(requiredBytes, availableStorage, storageLimit);
        }
    }

    /**
     * Update storage used after successful uploads (or reclaim on delete).
     * Handles both individual user and team storage.
     *
     * @param user        The user who uploaded/deleted
     * @param bytesChange The change in bytes (positive for upload, negative for
     *                    delete)
     */
    @Transactional
    public void updateStorageUsed(User user, long bytesChange) {
        // Check if user is part of a team first
        List<TeamMember> teamMemberships = teamMemberRepository.findByUserId(user.getId());
        Optional<TeamMember> teamMembership = teamMemberships.stream().findFirst();

        if (teamMembership.isPresent()) {
            Team team = teamMembership.get().getTeam();
            if (team.isAccessAllowed()) {
                updateTeamStorageUsed(team, bytesChange);
                return;
            }
        }

        updateIndividualStorageUsed(user, bytesChange);
    }

    /**
     * Update individual user's storage usage.
     */
    @Transactional
    public void updateIndividualStorageUsed(User user, long bytesChange) {
        Optional<UserSubscription> subscriptionOpt = userSubscriptionRepository.findByUserId(user.getId());

        if (subscriptionOpt.isEmpty()) {
            return;
        }

        UserSubscription subscription = subscriptionOpt.get();

        Long currentUsage = subscription.getStorageUsed();
        long newUsage = Math.max(0, currentUsage + bytesChange); // Never go below 0

        subscription.setStorageUsed(newUsage);
        userSubscriptionRepository.save(subscription);

        log.info("Updated storage for user {}: {} -> {} (change: {})",
                sanitizer.sanitizeLogging(user.getId()),
                sanitizer.sanitizeLoggingObject(formatBytes(currentUsage)),
                sanitizer.sanitizeLoggingObject(formatBytes(newUsage)),
                sanitizer.sanitizeLoggingObject(formatBytes(bytesChange)));
    }

    /**
     * Update team's storage usage.
     */
    @Transactional
    public void updateTeamStorageUsed(Team team, long bytesChange) {
        Long currentUsage = team.getStorageUsed();
        long newUsage = Math.max(0, currentUsage + bytesChange); // Never go below 0

        team.setStorageUsed(newUsage);
        teamRepository.save(team);

        log.info("Updated storage for team {}: {} -> {} (change: {})",
                sanitizer.sanitizeLogging(team.getId()),
                sanitizer.sanitizeLoggingObject(formatBytes(currentUsage)),
                sanitizer.sanitizeLoggingObject(formatBytes(newUsage)),
                sanitizer.sanitizeLoggingObject(formatBytes(bytesChange)));
    }

    /**
     * Update OCR pages used for a user (resets monthly).
     *
     * @param userId The ID of the user
     * @param pages  Number of pages processed
     */
    @Transactional
    public void updateOcrUsage(String userId, int pages) {
        if (pages <= 0)
            return;

        Optional<UserSubscription> subscriptionOpt = userSubscriptionRepository.findByUserId(userId);
        if (subscriptionOpt.isEmpty()) {
            return;
        }

        UserSubscription subscription = subscriptionOpt.get();
        int currentUsage = subscription.getOcrPagesUsed() != null ? subscription.getOcrPagesUsed() : 0;
        subscription.setOcrPagesUsed(currentUsage + pages);

        // Initialize quota reset date if not set
        initializeQuotaResetDateIfNeeded(subscription);

        userSubscriptionRepository.save(subscription);
    }

    /**
     * Update monthly documents uploaded count for a user (resets monthly).
     *
     * @param userId         The ID of the user
     * @param documentsCount Number of documents uploaded
     */
    @Transactional
    public void updateMonthlyDocumentsUploaded(String userId, int documentsCount) {
        if (documentsCount <= 0)
            return;

        Optional<UserSubscription> subscriptionOpt = userSubscriptionRepository.findByUserId(userId);
        if (subscriptionOpt.isEmpty()) {
            return;
        }

        UserSubscription subscription = subscriptionOpt.get();
        int currentCount = subscription.getMonthlyDocumentsUploaded() != null
                ? subscription.getMonthlyDocumentsUploaded() : 0;
        subscription.setMonthlyDocumentsUploaded(currentCount + documentsCount);

        // Initialize quota reset date if not set
        initializeQuotaResetDateIfNeeded(subscription);

        userSubscriptionRepository.save(subscription);
        log.debug("Updated monthly documents uploaded for user {}: {} -> {} (+{})",
                userId, currentCount, currentCount + documentsCount, documentsCount);
    }

    /**
     * Initialize the quota reset date if it hasn't been set yet.
     */
    private void initializeQuotaResetDateIfNeeded(UserSubscription subscription) {
        if (subscription.getQuotaResetDate() == null) {
            java.time.OffsetDateTime nextResetDate = java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC)
                    .with(java.time.temporal.TemporalAdjusters.firstDayOfNextMonth())
                    .withHour(0)
                    .withMinute(0)
                    .withSecond(0)
                    .withNano(0);
            subscription.setQuotaResetDate(nextResetDate);
        }
    }

    /**
     * Get storage information for a user.
     *
     * @param user The user to get storage info for
     * @return StorageInfo containing usage and limits
     */
    @Transactional(readOnly = true)
    public StorageInfo getStorageInfo(User user) {
        // Check if user is part of a team first
        List<TeamMember> teamMemberships = teamMemberRepository.findByUserId(user.getId());
        Optional<TeamMember> teamMembership = teamMemberships.stream().findFirst();

        if (teamMembership.isPresent()) {
            Team team = teamMembership.get().getTeam();
            if (team.isAccessAllowed()) {
                return getTeamStorageInfo(team, user);
            }
        }

        return getIndividualStorageInfo(user);
    }

    /**
     * Get storage info for individual user.
     */
    public StorageInfo getIndividualStorageInfo(User user) {
        // Fetch subscription with plan eagerly to avoid lazy loading issues
        Optional<UserSubscription> subscriptionOpt = userSubscriptionRepository.findByUserIdWithPlan(user.getId());

        if (subscriptionOpt.isEmpty()) {
            return StorageInfo.builder()
                    .storageUsed(0L)
                    .storageLimit(0L)
                    .storageUsedFormatted("0 B")
                    .storageLimitFormatted("0 B")
                    .percentageUsed(0.0)
                    .isUnlimited(false)
                    .ocrPageLimit(0)
                    .ocrPagesUsed(0)
                    .ocrPagesRemaining(0)
                    .ocrUnlimited(false)
                    .documentUploadLimit(0)
                    .documentsUploaded(0)
                    .documentsRemaining(0)
                    .documentsUnlimited(false)
                    .subscriptionPlan("None")
                    .billingInterval("N/A")
                    .build();
        }

        UserSubscription subscription = subscriptionOpt.get();

        SubscriptionPlan plan = subscription.getPlan();
        Long storageLimit = plan.getStorageLimit();
        Long storageUsed = subscription.getStorageUsed();

        // OCR info (resets monthly)
        Integer ocrPageLimit = plan.getOcrPageLimit();
        Integer ocrPagesUsed = subscription.getOcrPagesUsed() != null ? subscription.getOcrPagesUsed() : 0;
        boolean ocrUnlimited = ocrPageLimit == null || ocrPageLimit == 0;
        Integer ocrPagesRemaining = ocrUnlimited ? null : Math.max(0, ocrPageLimit - ocrPagesUsed);

        // Document upload info - use monthly count (resets monthly)
        Integer documentUploadLimit = plan.getDocumentUploadLimit();
        int documentsUploaded = subscription.getMonthlyDocumentsUploaded() != null
                ? subscription.getMonthlyDocumentsUploaded() : 0;
        boolean documentsUnlimited = documentUploadLimit == null || documentUploadLimit == 0;
        Integer documentsRemaining = documentsUnlimited ? null : Math.max(0, documentUploadLimit - documentsUploaded);

        // Subscription plan info
        String subscriptionPlanName = plan.getName() != null ? plan.getName().getPlanName() : "Unknown";
        String billingInterval = formatBillingInterval(plan);

        // Quota reset date
        OffsetDateTime quotaResetDate = subscription.getQuotaResetDate();

        return buildStorageInfo(storageUsed, storageLimit, ocrPageLimit, ocrPagesUsed, ocrPagesRemaining,
                ocrUnlimited, documentUploadLimit, documentsUploaded, documentsRemaining, documentsUnlimited,
                subscriptionPlanName, billingInterval, quotaResetDate);
    }

    /**
     * Get storage info for a team.
     */
    public StorageInfo getTeamStorageInfo(Team team, User user) {
        TeamSubscriptionPlan plan = team.getPlan();
        Long storageLimit = plan != null ? plan.getStorageLimit() : null;
        Long storageUsed = team.getStorageUsed();

        // OCR info - teams may not have OCR limits in the same way
        Integer ocrPageLimit = null; // Team plans don't have individual OCR limits per user
        Integer ocrPagesUsed = 0;
        boolean ocrUnlimited = true;
        Integer ocrPagesRemaining = null;

        // Document upload info for the user within the team (count in real-time)
        Integer documentUploadLimit = plan != null ? plan.getMonthlyDocumentLimit() : null;
        Long documentsUploadedLong = documentCollectionRepository.countByUserId(user.getId());
        Integer documentsUploaded = documentsUploadedLong != null ? documentsUploadedLong.intValue() : 0;
        boolean documentsUnlimited = documentUploadLimit == null;
        Integer documentsRemaining = documentsUnlimited ? null : Math.max(0, documentUploadLimit - documentsUploaded);

        // Subscription plan info
        String subscriptionPlanName = plan != null ? plan.getDisplayName() : "Unknown";
        String billingInterval = "Team Plan";

        // Team users don't have individual quota reset dates
        return buildStorageInfo(storageUsed, storageLimit, ocrPageLimit, ocrPagesUsed, ocrPagesRemaining,
                ocrUnlimited, documentUploadLimit, documentsUploaded, documentsRemaining, documentsUnlimited,
                subscriptionPlanName, billingInterval, null);
    }

    /**
     * Format billing interval from plan.
     */
    private String formatBillingInterval(SubscriptionPlan plan) {
        if (plan.getBillingIntervalUnit() == null) {
            return "N/A";
        }
        String unit = plan.getBillingIntervalUnit().name().toLowerCase();
        int value = plan.getBillingIntervalValue() != null ? plan.getBillingIntervalValue() : 1;
        if (value == 1) {
            return unit.substring(0, 1).toUpperCase() + unit.substring(1) + "ly";
        }
        return value + " " + unit + "s";
    }

    /**
     * Build StorageInfo object from all usage and limit values.
     */
    private StorageInfo buildStorageInfo(Long storageUsed, Long storageLimit,
            Integer ocrPageLimit, Integer ocrPagesUsed, Integer ocrPagesRemaining,
            boolean ocrUnlimited, Integer documentUploadLimit, Integer documentsUploaded,
            Integer documentsRemaining, boolean documentsUnlimited,
            String subscriptionPlan, String billingInterval,
            OffsetDateTime quotaResetDate) {
        boolean isUnlimited = storageLimit == null;
        double percentageUsed = 0.0;
        long safeStorageUsed = storageUsed != null ? storageUsed : 0L;
        long safeStorageLimit = storageLimit != null ? storageLimit : 0L;

        if (!isUnlimited && safeStorageLimit > 0) {
            percentageUsed = (safeStorageUsed * 100.0) / safeStorageLimit;
        }

        return StorageInfo.builder()
                .storageUsed(safeStorageUsed)
                .storageLimit(storageLimit)
                .storageUsedFormatted(formatBytes(safeStorageUsed))
                .storageLimitFormatted(isUnlimited ? "Unlimited" : formatBytes(safeStorageLimit))
                .percentageUsed(Math.round(percentageUsed * 100.0) / 100.0)
                .isUnlimited(isUnlimited)
                .ocrPageLimit(ocrPageLimit)
                .ocrPagesUsed(ocrPagesUsed)
                .ocrPagesRemaining(ocrPagesRemaining)
                .ocrUnlimited(ocrUnlimited)
                .documentUploadLimit(documentUploadLimit)
                .documentsUploaded(documentsUploaded)
                .documentsRemaining(documentsRemaining)
                .documentsUnlimited(documentsUnlimited)
                .subscriptionPlan(subscriptionPlan)
                .billingInterval(billingInterval)
                .quotaResetDate(quotaResetDate)
                .build();
    }

    /**
     * Format bytes to human-readable string.
     */
    public static String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        } else if (bytes < 1024L * 1024 * 1024) {
            return String.format("%.2f MB", bytes / (1024.0 * 1024));
        } else {
            return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
        }
    }
}
