package com.dogetennant.dplayerprofiles.achievement;

import com.dogetennant.dplayerprofiles.DPlayerProfiles;
import com.dogetennant.dplayerprofiles.Fakes;
import com.dogetennant.dplayerprofiles.config.AchievementConfigLoader;
import com.dogetennant.dplayerprofiles.config.MainConfig;
import com.dogetennant.dplayerprofiles.model.AchievementConfig;
import com.dogetennant.dplayerprofiles.model.PlayerProfile;
import com.dogetennant.dplayerprofiles.model.TriggerType;
import com.dogetennant.dplayerprofiles.player.ProfileManager;
import com.dogetennant.dplayerprofiles.reward.RewardEntry;
import com.dogetennant.dplayerprofiles.reward.RewardManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** How progress adds up, when an achievement completes, and what completing it gives. */
class AchievementManagerTest {

    private static final UUID ALEX = UUID.fromString("00000000-0000-0000-0000-00000000a1e7");

    private final MainConfig config = Fakes.config();
    private final DPlayerProfiles plugin = Fakes.plugin(config);
    private final Map<String, AchievementConfig> achievements = new LinkedHashMap<>();
    private final RewardManager rewards = mock(RewardManager.class);
    private final PlayerProfile profile = new PlayerProfile(ALEX, "Alex", 0, 0, 0, 1, "2026-10-09");
    private final Player alex = mock(Player.class);
    private AchievementManager manager;

    @BeforeEach
    void setUp() {
        AchievementConfigLoader loader = mock(AchievementConfigLoader.class);
        when(loader.getAll()).thenReturn(achievements);
        when(loader.isWatched(any(), any())).thenReturn(true);
        ProfileManager profiles = mock(ProfileManager.class);
        when(profiles.get(ALEX)).thenReturn(profile);
        when(plugin.getProfileManager()).thenReturn(profiles);
        when(alex.getUniqueId()).thenReturn(ALEX);
        when(alex.getName()).thenReturn("Alex");
        when(alex.hasPermission(anyString())).thenReturn(false);
        manager = new AchievementManager(plugin, loader, rewards);
    }

    private AchievementConfig add(String id, TriggerType type, long count, String... targets) {
        AchievementConfig ac = new AchievementConfig();
        ac.id = id;
        ac.displayName = id;
        ac.icon = Material.PAPER;
        ac.requires = List.of();
        ac.requiredBadges = List.of();
        ac.permission = "";
        ac.triggerType = type;
        ac.triggerTarget = targets.length == 0 ? null : List.of(targets);
        ac.triggerCount = count;
        ac.rewards = List.of(RewardEntry.command("say {player} did it"));
        achievements.put(id, ac);
        return ac;
    }

    @Test
    void progressAddsUpAndCompletesOnceAtTheCount() {
        add("miner", TriggerType.BLOCK_BREAK, 3, "STONE");

        manager.increment(alex, TriggerType.BLOCK_BREAK, "STONE", 1);
        manager.increment(alex, TriggerType.BLOCK_BREAK, "STONE", 1);
        assertThat(profile.isCompleted("miner")).isFalse();
        manager.increment(alex, TriggerType.BLOCK_BREAK, "STONE", 1);
        manager.increment(alex, TriggerType.BLOCK_BREAK, "STONE", 5);

        assertThat(profile.isCompleted("miner")).isTrue();
        assertThat(profile.getProgress("miner")).isEqualTo(3);
        verify(rewards, times(1)).grant(any(), any());
    }

    @Test
    void onlyTheConfiguredTargetsCount() {
        add("granite", TriggerType.BLOCK_BREAK, 2, "GRANITE", "DIORITE");

        manager.increment(alex, TriggerType.BLOCK_BREAK, "STONE", 1);
        manager.increment(alex, TriggerType.BLOCK_BREAK, "diorite", 1);

        assertThat(profile.getProgress("granite")).isEqualTo(1);
    }

    @Test
    void aLockedAchievementGetsNoProgress() {
        AchievementConfig needsOther = add("second", TriggerType.FISH, 1);
        needsOther.requires = List.of("first");
        AchievementConfig needsPermission = add("vip", TriggerType.FISH, 1);
        needsPermission.permission = "server.vip";
        AchievementConfig needsBadge = add("badged", TriggerType.FISH, 1);
        needsBadge.requiredBadges = List.of("angler");

        manager.increment(alex, TriggerType.FISH, null, 1);

        assertThat(profile.getProgress("second")).isZero();
        assertThat(profile.getProgress("vip")).isZero();
        assertThat(profile.getProgress("badged")).isZero();
    }

    @Test
    void aChainCompletesWhenWhatItRequiresIsDone() {
        add("first", TriggerType.FISH, 1);
        AchievementConfig chain = add("master", TriggerType.CHAIN, 1);
        chain.requires = List.of("first");

        manager.increment(alex, TriggerType.FISH, null, 1);

        assertThat(profile.isCompleted("master")).isTrue();
        verify(rewards, times(2)).grant(any(), any());
    }

    @Test
    void aLoginStreakIsTheCurrentStreak() {
        add("week", TriggerType.LOGIN_STREAK, 7);

        manager.increment(alex, TriggerType.LOGIN_STREAK, null, 5);
        manager.increment(alex, TriggerType.LOGIN_STREAK, null, 1);    // the streak broke

        assertThat(profile.getProgress("week")).isEqualTo(1);
    }

    @Test
    void anyOtherSkillLevellingUpDoesNotLowerTheProgressOfAnAnySkillGoal() {
        add("expert", TriggerType.MCMMO_LEVEL_UP, 50, "*");

        manager.increment(alex, TriggerType.MCMMO_LEVEL_UP, "MINING", 40);
        manager.increment(alex, TriggerType.MCMMO_LEVEL_UP, "WOODCUTTING", 5);

        assertThat(profile.getProgress("expert")).isEqualTo(40);
    }

    @Test
    void pointsUnlockEveryNodeTheyPassOnce() {
        config.pointsEnabled = true;
        config.pointsPerNode = 10;
        config.pointsNodeCount = 3;
        add("small", TriggerType.FISH, 1).points = 25;
        add("big", TriggerType.CRAFT, 1).points = 30;

        manager.increment(alex, TriggerType.FISH, null, 1);      // 25 points: nodes 1 and 2
        manager.increment(alex, TriggerType.CRAFT, null, 1);     // 55 points: node 3 (the last one)

        var storage = plugin.getAchievementRewardStorage();
        verify(storage).getRewards("points_node_0");
        verify(storage).getRewards("points_node_1");
        verify(storage).getRewards("points_node_2");
        verify(storage, never()).getRewards("points_node_3");
    }

    @Test
    void forceCompletingSkipsTheCountButNotTheRewards() {
        AchievementConfig ac = add("event", TriggerType.MANUAL, 5);

        manager.forceComplete(alex, profile, ac);
        manager.forceComplete(alex, profile, ac);

        assertThat(profile.isCompleted("event")).isTrue();
        verify(rewards, times(1)).grant(any(), any());
    }
}
