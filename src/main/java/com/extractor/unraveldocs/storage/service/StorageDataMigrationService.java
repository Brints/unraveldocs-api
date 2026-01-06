package com.extractor.unraveldocs.storage.service;

import com.extractor.unraveldocs.documents.datamodel.DocumentUploadState;
import com.extractor.unraveldocs.documents.model.DocumentCollection;
import com.extractor.unraveldocs.documents.model.FileEntry;
import com.extractor.unraveldocs.subscription.model.UserSubscription;
import com.extractor.unraveldocs.subscription.repository.UserSubscriptionRepository;
import com.extractor.unraveldocs.team.model.Team;
import com.extractor.unraveldocs.team.model.TeamMember;
import com.extractor.unraveldocs.team.repository.TeamMemberRepository;
import com.extractor.unraveldocs.team.repository.TeamRepository;
import com.extractor.unraveldocs.user.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Service for migrating storage data.
 * Calculates and updates storage usage for existing users and teams based on
 * their current documents.
 * This is intended to be run once after deploying the storage allocation
 * feature to backfill
 * historical storage usage data.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StorageDataMigrationService {

    private final UserSubscriptionRepository userSubscriptionRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;

    /**
     * Migrate storage data for all users and teams.
     * Calculates current storage usage based on existing documents.
     *
     * @return A summary of the migration results
     */
    @Transactional
    public MigrationResult migrateStorageData() {
        log.info("Starting storage data migration...");

        int usersUpdated = 0;
        int teamsUpdated = 0;
        long totalBytesCalculated = 0;

        // First, calculate storage for team members (they contribute to team storage)
        Map<String, Long> teamStorageMap = new HashMap<>();

        // Get all users with subscriptions
        List<UserSubscription> subscriptions = userSubscriptionRepository.findAll();

        for (UserSubscription subscription : subscriptions) {
            User user = subscription.getUser();
            Set<DocumentCollection> collections = user.getDocuments();

            if (collections == null || collections.isEmpty()) {
                // User has no documents, ensure storage is set to 0
                if (subscription.getStorageUsed() != 0L) {
                    subscription.setStorageUsed(0L);
                    userSubscriptionRepository.save(subscription);
                    usersUpdated++;
                }
                continue;
            }

            // Calculate total storage used by this user
            long userStorageUsed = calculateStorageForCollections(collections);

            // Check if user is part of a team
            List<TeamMember> teamMemberships = teamMemberRepository.findByUserId(user.getId());

            if (!teamMemberships.isEmpty()) {
                // User is part of a team - add their storage to team total
                Team team = teamMemberships.getFirst().getTeam();
                String teamId = team.getId();
                teamStorageMap.merge(teamId, userStorageUsed, Long::sum);
            } else {
                // Individual user - update their subscription storage
                if (!subscription.getStorageUsed().equals(userStorageUsed)) {
                    subscription.setStorageUsed(userStorageUsed);
                    userSubscriptionRepository.save(subscription);
                    usersUpdated++;
                    totalBytesCalculated += userStorageUsed;
                    log.info("Updated storage for user {}: {} bytes ({})",
                            user.getId(), userStorageUsed, formatBytes(userStorageUsed));
                }
            }
        }

        // Update team storage
        for (Map.Entry<String, Long> entry : teamStorageMap.entrySet()) {
            String teamId = entry.getKey();
            Long storageUsed = entry.getValue();

            teamRepository.findById(teamId).ifPresent(team -> {
                if (!team.getStorageUsed().equals(storageUsed)) {
                    team.setStorageUsed(storageUsed);
                    teamRepository.save(team);
                    log.info("Updated storage for team {}: {} bytes ({})",
                            teamId, storageUsed, formatBytes(storageUsed));
                }
            });
            teamsUpdated++;
            totalBytesCalculated += storageUsed;
        }

        log.info("Storage data migration completed. Users updated: {}, Teams updated: {}, Total bytes: {} ({})",
                usersUpdated, teamsUpdated, totalBytesCalculated, formatBytes(totalBytesCalculated));

        return new MigrationResult(usersUpdated, teamsUpdated, totalBytesCalculated);
    }

    /**
     * Calculate total storage used by a set of document collections.
     */
    private long calculateStorageForCollections(Set<DocumentCollection> collections) {
        return collections.stream()
                .flatMap(collection -> collection.getFiles().stream())
                .filter(file -> DocumentUploadState.SUCCESS.toString().equals(file.getUploadStatus()))
                .mapToLong(FileEntry::getFileSize)
                .sum();
    }

    /**
     * Format bytes to human-readable string.
     */
    private String formatBytes(long bytes) {
        return StorageAllocationService.formatBytes(bytes);
    }

    /**
     * Result of the migration operation.
     */
    public record MigrationResult(int usersUpdated, int teamsUpdated, long totalBytesCalculated) {

        public String getSummary() {
            return String.format("Migration completed: %d users updated, %d teams updated, %s total storage calculated",
                    usersUpdated, teamsUpdated, StorageAllocationService.formatBytes(totalBytesCalculated));
        }
    }
}
