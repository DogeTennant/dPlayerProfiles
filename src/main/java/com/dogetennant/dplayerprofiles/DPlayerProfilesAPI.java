package com.dogetennant.dplayerprofiles;

import com.dogetennant.dplayerprofiles.achievement.AchievementManager;
import com.dogetennant.dplayerprofiles.config.AchievementConfigLoader;
import com.dogetennant.dplayerprofiles.config.BadgeConfigLoader;
import com.dogetennant.dplayerprofiles.model.AchievementConfig;
import com.dogetennant.dplayerprofiles.model.BadgeConfig;
import com.dogetennant.dplayerprofiles.model.PlayerProfile;
import com.dogetennant.dplayerprofiles.player.ProfileManager;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;

/**
 * Public API for dPlayerProfiles.
 * Obtain via: DPlayerProfilesAPI api = (DPlayerProfilesAPI) Bukkit.getPluginManager().getPlugin("dPlayerProfiles");
 * Or more conveniently, cast the Plugin to DPlayerProfiles and call getAPI().
 */
public class DPlayerProfilesAPI {

    private final ProfileManager profileManager;
    private final AchievementManager achievementManager;
    private final AchievementConfigLoader achievementConfigLoader;
    private final BadgeConfigLoader badgeConfigLoader;

    DPlayerProfilesAPI(ProfileManager profileManager, AchievementManager achievementManager,
                        AchievementConfigLoader achievementConfigLoader, BadgeConfigLoader badgeConfigLoader) {
        this.profileManager = profileManager;
        this.achievementManager = achievementManager;
        this.achievementConfigLoader = achievementConfigLoader;
        this.badgeConfigLoader = badgeConfigLoader;
    }

    /** Returns the cached profile for an online player, or null if not loaded. */
    public PlayerProfile getProfile(UUID uuid) {
        return profileManager.get(uuid);
    }

    /**
     * Increments progress toward a specific achievement by ID.
     * The player must be online and their profile must be loaded.
     */
    public void incrementProgress(Player player, String achievementId, long amount) {
        AchievementConfig ac = achievementConfigLoader.get(achievementId);
        if (ac == null) return;
        PlayerProfile profile = profileManager.get(player.getUniqueId());
        if (profile == null) return;
        long newProgress = profile.getProgress(achievementId) + amount;
        long completedAt = 0;
        if (newProgress >= ac.triggerCount) {
            newProgress = ac.triggerCount;
            completedAt = System.currentTimeMillis();
        }
        profile.setProgress(achievementId, newProgress, completedAt);
        profileManager.saveProgress(player.getUniqueId(), achievementId, newProgress, completedAt);
        if (completedAt > 0) {
            achievementManager.forceComplete(player, profile, ac);
        }
    }

    /** Force-completes an achievement for an online player, firing rewards and notifications. */
    public void forceComplete(Player player, String achievementId) {
        AchievementConfig ac = achievementConfigLoader.get(achievementId);
        if (ac == null) return;
        PlayerProfile profile = profileManager.get(player.getUniqueId());
        if (profile == null) return;
        achievementManager.forceComplete(player, profile, ac);
    }

    /** Grants a badge to an online player. */
    public void grantBadge(Player player, String badgeId) {
        BadgeConfig bc = badgeConfigLoader.get(badgeId);
        if (bc == null) return;
        PlayerProfile profile = profileManager.get(player.getUniqueId());
        if (profile == null || profile.hasBadge(badgeId)) return;
        long now = System.currentTimeMillis();
        profile.grantBadge(badgeId, now);
        profileManager.saveBadge(player.getUniqueId(), badgeId, now, null);
    }

    /** Revokes a badge from an online player. */
    public void revokeBadge(Player player, String badgeId) {
        PlayerProfile profile = profileManager.get(player.getUniqueId());
        if (profile == null || !profile.hasBadge(badgeId)) return;
        profile.revokeBadge(badgeId);
        profileManager.removeBadge(player.getUniqueId(), badgeId);
    }

    public Map<String, AchievementConfig> getAchievements() {
        return achievementConfigLoader.getAll();
    }

    public Map<String, BadgeConfig> getBadges() {
        return badgeConfigLoader.getAll();
    }
}
