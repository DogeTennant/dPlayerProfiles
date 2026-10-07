package com.dogetennant.dplayerprofiles.model;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PlayerProfileTest {

    private static PlayerProfile profile() {
        return new PlayerProfile(UUID.randomUUID(), "Alex", 0, 0, 0, 0, null);
    }

    @Test
    void chatShowsThePinnedBadgesThePlayerStillHas() {
        PlayerProfile profile = profile();
        profile.grantBadge("veteran", 1);
        profile.grantBadge("builder", 2);
        profile.setPinnedBadges(List.of("builder", "gone", "veteran"));

        assertThat(profile.getEffectiveChatBadges()).containsExactly("builder", "veteran");
    }

    @Test
    void withoutPinsChatShowsTheFirstThreeEarned() {
        PlayerProfile profile = profile();
        profile.grantBadge("d", 4);
        profile.grantBadge("a", 1);
        profile.grantBadge("c", 3);
        profile.grantBadge("b", 2);

        assertThat(profile.getEffectiveChatBadges()).containsExactly("a", "b", "c");
    }

    @Test
    void badgeIdsIgnoreLetterCase() {
        PlayerProfile profile = profile();
        profile.grantBadge("Veteran", 1);

        assertThat(profile.hasBadge("VETERAN")).isTrue();
        assertThat(profile.getBadges()).containsOnlyKeys("veteran");
        assertThat(profile.revokeBadge("veTERan")).isTrue();
        assertThat(profile.hasBadge("veteran")).isFalse();
    }

    @Test
    void onlyCompletedAchievementsCount() {
        PlayerProfile profile = profile();
        profile.setProgress("miner", 50, 0);
        profile.setProgress("fisher", 10, 1234);

        assertThat(profile.completedAchievementCount()).isEqualTo(1);
        assertThat(profile.getProgress("miner")).isEqualTo(50);
        assertThat(profile.getProgress("unknown")).isZero();
        assertThat(profile.isCompleted("fisher")).isTrue();
    }
}
