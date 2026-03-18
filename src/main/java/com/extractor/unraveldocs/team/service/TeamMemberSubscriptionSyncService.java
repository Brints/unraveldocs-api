package com.extractor.unraveldocs.team.service;

import com.extractor.unraveldocs.team.model.Team;
import com.extractor.unraveldocs.user.model.User;

public interface TeamMemberSubscriptionSyncService {
    void downgradeTeamMembers(Team team);

    void upgradeTeamMember(User user, Team team);

    void revertMemberSubscription(User user);
}

