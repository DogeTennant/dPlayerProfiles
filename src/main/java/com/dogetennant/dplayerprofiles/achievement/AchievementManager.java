package com.dogetennant.dplayerprofiles.achievement;

import com.dogetennant.dplayerprofiles.DPlayerProfiles;
import com.dogetennant.dplayerprofiles.config.AchievementConfigLoader;
import com.dogetennant.dplayerprofiles.lang.MessageKey;
import com.dogetennant.dplayerprofiles.lang.Placeholder;
import com.dogetennant.dplayerprofiles.model.AchievementConfig;
import com.dogetennant.dplayerprofiles.model.PlayerProfile;
import com.dogetennant.dplayerprofiles.model.TriggerType;
import com.dogetennant.dplayerprofiles.player.ProfileManager;
import com.dogetennant.dplayerprofiles.reward.RewardEntry;
import com.dogetennant.dplayerprofiles.reward.RewardManager;
import com.dogetennant.dplayerprofiles.util.ColorUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;

public class AchievementManager {

    private final DPlayerProfiles plugin;
    private final AchievementConfigLoader configLoader;
    private final RewardManager rewardManager;

    public AchievementManager(DPlayerProfiles plugin, AchievementConfigLoader configLoader,
                               RewardManager rewardManager) {
        this.plugin = plugin;
        this.configLoader = configLoader;
        this.rewardManager = rewardManager;
    }

    public void onLogin(Player player, PlayerProfile profile) {
        increment(player, TriggerType.LOGIN_COUNT, null, 1);
        if (plugin.getConfigManager().get().streaksEnabled) {
            increment(player, TriggerType.LOGIN_STREAK, null, profile.getLoginStreak());
        }
        // Login may unlock chain achievements (e.g., required badge already held)
        checkChainAchievements(player, profile);
    }

    /**
     * Increments progress for all achievements matching the given trigger and target.
     * CHAIN-type achievements are skipped here - they auto-complete via {@link #checkChainAchievements}.
     * For LOGIN_STREAK the amount is the current streak value (not a delta).
     */
    public void increment(Player player, TriggerType type, String target, long amount) {
        ProfileManager profileManager = plugin.getProfileManager();
        PlayerProfile profile = profileManager.get(player.getUniqueId());
        if (profile == null) return;

        for (AchievementConfig ac : configLoader.getAll().values()) {
            if (ac.triggerType == TriggerType.CHAIN) continue; // handled separately
            if (ac.triggerType != type) continue;
            if (!targetMatches(ac.triggerTarget, target)) continue;
            if (profile.isCompleted(ac.id)) continue;
            if (!ac.permission.isBlank() && !player.hasPermission(ac.permission)) continue;
            if (!categoryPermissionMet(player, ac)) continue;
            if (!prerequisitesMet(profile, ac)) continue;
            if (!badgeRequirementsMet(profile, ac)) continue;

            long newProgress;
            if (type == TriggerType.LOGIN_STREAK || type == TriggerType.MCMMO_LEVEL_UP
                    || type == TriggerType.JOBS_LEVEL_UP || type == TriggerType.AURASKILLS_LEVEL_UP) {
                newProgress = amount; // SET semantics: amount is the current value, not a delta
            } else {
                newProgress = profile.getProgress(ac.id) + amount;
            }

            long completedAt = 0;
            if (newProgress >= ac.triggerCount) {
                newProgress = ac.triggerCount;
                completedAt = System.currentTimeMillis();
            }

            profile.setProgress(ac.id, newProgress, completedAt);
            profileManager.saveProgress(player.getUniqueId(), ac.id, newProgress, completedAt);

            if (completedAt > 0) {
                onComplete(player, profile, ac);
            }
        }
    }

    /** Force-completes an achievement regardless of progress or prerequisites. */
    public void forceComplete(Player player, PlayerProfile profile, AchievementConfig ac) {
        if (profile.isCompleted(ac.id)) return;
        long now = System.currentTimeMillis();
        profile.setProgress(ac.id, ac.triggerCount, now);
        plugin.getProfileManager().saveProgress(player.getUniqueId(), ac.id, ac.triggerCount, now);
        onComplete(player, profile, ac);
    }

    /**
     * Checks all CHAIN-type achievements and auto-completes any whose requirements are now satisfied.
     * Should be called after any achievement or badge is granted to the player.
     */
    public void checkChainAchievements(Player player, PlayerProfile profile) {
        for (AchievementConfig ac : configLoader.getAll().values()) {
            if (ac.triggerType != TriggerType.CHAIN) continue;
            if (profile.isCompleted(ac.id)) continue;
            if (!ac.permission.isBlank() && !player.hasPermission(ac.permission)) continue;
            if (!categoryPermissionMet(player, ac)) continue;
            if (!prerequisitesMet(profile, ac)) continue;
            if (!badgeRequirementsMet(profile, ac)) continue;
            forceComplete(player, profile, ac);
        }
    }

    private void onComplete(Player player, PlayerProfile profile, AchievementConfig ac) {
        rewardManager.grant(player, ac.rewards);
        notifyPlayer(player, ac);
        if (ac.broadcast) {
            broadcast(player.getName(), ac);
        }
        if (ac.points > 0) {
            checkForNewNodes(player, profile, ac.points);
        }
        // A completed achievement may satisfy chain requirements
        checkChainAchievements(player, profile);
    }

    /** Sums points for all completed achievements in the player's profile. */
    public int calculateTotalPoints(PlayerProfile profile) {
        int total = 0;
        for (AchievementConfig ac : configLoader.getAll().values()) {
            if (profile.isCompleted(ac.id)) total += ac.points;
        }
        return total;
    }

    private void checkForNewNodes(Player player, PlayerProfile profile, int earnedPoints) {
        if (!plugin.getConfigManager().get().pointsEnabled) return;
        int pointsPerNode = plugin.getConfigManager().get().pointsPerNode;
        int nodeCount = plugin.getConfigManager().get().pointsNodeCount;
        if (pointsPerNode <= 0 || nodeCount <= 0) return;

        int newTotal = calculateTotalPoints(profile);
        int oldTotal = newTotal - earnedPoints;

        int newUnlocked = Math.min(newTotal / pointsPerNode, nodeCount);
        int oldUnlocked = Math.min(Math.max(oldTotal, 0) / pointsPerNode, nodeCount);

        for (int i = oldUnlocked; i < newUnlocked; i++) {
            List<RewardEntry> nodeRewards = plugin.getAchievementRewardStorage().getRewards("points_node_" + i);
            if (!nodeRewards.isEmpty()) {
                rewardManager.grant(player, nodeRewards);
            }
            plugin.getLangManager().send(player, MessageKey.POINTS_NODE_UNLOCKED_CHAT,
                    Placeholder.of("node", i + 1));
        }
    }

    private void notifyPlayer(Player player, AchievementConfig ac) {
        String mode = plugin.getConfigManager().get().achievementNotification;

        if (mode.equals("title") || mode.equals("both")) {
            Component title = plugin.getLangManager().get(MessageKey.ACHIEVEMENT_COMPLETE_TITLE);
            Component subtitle = plugin.getLangManager().get(MessageKey.ACHIEVEMENT_COMPLETE_SUBTITLE,
                    Placeholder.of("achievement", ac.displayName));
            player.showTitle(net.kyori.adventure.title.Title.title(title, subtitle));
        }
        if (mode.equals("chat") || mode.equals("both")) {
            plugin.getLangManager().send(player, MessageKey.ACHIEVEMENT_COMPLETE_CHAT,
                    Placeholder.of("achievement", ac.displayName));
        }
    }

    private void broadcast(String playerName, AchievementConfig ac) {
        Component msg = plugin.getLangManager().get(MessageKey.ACHIEVEMENT_BROADCAST,
                Placeholder.of("player", playerName),
                Placeholder.of("achievement", ac.displayName));
        Bukkit.broadcast(msg);
    }

    private boolean targetMatches(String configTarget, String eventTarget) {
        if (configTarget == null || configTarget.equals("*")) return true;
        if (eventTarget == null) return false;
        return configTarget.equalsIgnoreCase(eventTarget);
    }

    private boolean prerequisitesMet(PlayerProfile profile, AchievementConfig ac) {
        for (String req : ac.requires) {
            if (!profile.isCompleted(req)) return false;
        }
        return true;
    }

    private boolean categoryPermissionMet(Player player, AchievementConfig ac) {
        if (ac.category == null) return true;
        String required = plugin.getConfigManager().get().categoryPermissions.get(ac.category);
        return required == null || player.hasPermission(required);
    }

    private boolean badgeRequirementsMet(PlayerProfile profile, AchievementConfig ac) {
        for (String badgeId : ac.requiredBadges) {
            if (!profile.hasBadge(badgeId)) return false;
        }
        return true;
    }
}
