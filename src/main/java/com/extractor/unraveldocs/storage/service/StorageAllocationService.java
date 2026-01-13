package com.extractor.unraveldocs.storage.service;

import com.extractor.unraveldocs.documents.repository.DocumentCollectionRepository;
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
            checkTeamStorageAvailable(team, requiredBytes);
        } else {
            checkIndividualStorageAvailable(user, requiredBytes);
        }
    }

    /**
     * Check if individual user has sufficient storage available.
     */
    private void checkIndividualStorageAvailable(User user, long requiredBytes) {
        UserSubscription subscription = user.getSubscription();
        if (subscription == null) {
            throw new StorageQuotaExceededException("No active subscription found. Please subscribe to a plan.");
        }

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
            log.warn("Storage quota exceeded for user {}. Required: {}, Available: {}, Limit: {}",
                    user.getId(), requiredBytes, availableStorage, storageLimit);
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
            log.warn("Team storage quota exceeded for team {}. Required: {}, Available: {}, Limit: {}",
                    team.getId(), requiredBytes, availableStorage, storageLimit);
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
            updateTeamStorageUsed(team, bytesChange);
        } else {
            updateIndividualStorageUsed(user, bytesChange);
        }
    }

    /**
     * Update individual user's storage usage.
     */
    @Transactional
    public void updateIndividualStorageUsed(User user, long bytesChange) {
        UserSubscription subscription = user.getSubscription();
        if (subscription == null) {
            log.warn("Cannot update storage for user {} - no subscription found", user.getId());
            return;
        }

        Long currentUsage = subscription.getStorageUsed();
        long newUsage = Math.max(0, currentUsage + bytesChange); // Never go below 0

        subscription.setStorageUsed(newUsage);
        userSubscriptionRepository.save(subscription);

        log.info("Updated storage for user {}: {} -> {} (change: {})",
                user.getId(), formatBytes(currentUsage), formatBytes(newUsage), formatBytes(bytesChange));
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
                team.getId(), formatBytes(currentUsage), formatBytes(newUsage), formatBytes(bytesChange));
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
            return getTeamStorageInfo(team, user);
        } else {
            return getIndividualStorageInfo(user);
        }
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

        // OCR info
        Integer ocrPageLimit = plan.getOcrPageLimit();
        Integer ocrPagesUsed = subscription.getOcrPagesUsed() != null ? subscription.getOcrPagesUsed() : 0;
        boolean ocrUnlimited = ocrPageLimit == null || ocrPageLimit == 0;
        Integer ocrPagesRemaining = ocrUnlimited ? null : Math.max(0, ocrPageLimit - ocrPagesUsed);

        // Document upload info (count in real-time)
        Integer documentUploadLimit = plan.getDocumentUploadLimit();
        Long documentsUploadedLong = documentCollectionRepository.countByUserId(user.getId());
        Integer documentsUploaded = documentsUploadedLong != null ? documentsUploadedLong.intValue() : 0;
        boolean documentsUnlimited = documentUploadLimit == null || documentUploadLimit == 0;
        Integer documentsRemaining = documentsUnlimited ? null : Math.max(0, documentUploadLimit - documentsUploaded);

        // Subscription plan info
        String subscriptionPlanName = plan.getName() != null ? plan.getName().getPlanName() : "Unknown";
        String billingInterval = formatBillingInterval(plan);

        return buildStorageInfo(storageUsed, storageLimit, ocrPageLimit, ocrPagesUsed, ocrPagesRemaining,
                ocrUnlimited, documentUploadLimit, documentsUploaded, documentsRemaining, documentsUnlimited,
                subscriptionPlanName, billingInterval);
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

        return buildStorageInfo(storageUsed, storageLimit, ocrPageLimit, ocrPagesUsed, ocrPagesRemaining,
                ocrUnlimited, documentUploadLimit, documentsUploaded, documentsRemaining, documentsUnlimited,
                subscriptionPlanName, billingInterval);
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
            String subscriptionPlan, String billingInterval) {
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
