package com.dogetennant.dplayerprofiles.integration;

import com.dogetennant.dplayerprofiles.DPlayerProfiles;
import com.dogetennant.dplayerprofiles.model.PlayerProfile;
import com.dogetennant.dplayerprofiles.util.TimeUtil;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlaceholderAPIHook extends PlaceholderExpansion {

    private final DPlayerProfiles plugin;

    public PlaceholderAPIHook(DPlayerProfiles plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() { return "dpp"; }

    @Override
    public @NotNull String getAuthor() { return "dogetennant"; }

    @Override
    public @NotNull String getVersion() { return plugin.getDescription().getVersion(); }

    @Override
    public boolean persist() { return true; }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) return "";
        PlayerProfile profile = plugin.getProfileManager().get(player.getUniqueId());
        if (profile == null) return "0";

        return switch (params.toLowerCase()) {
            case "achievements_completed" -> String.valueOf(profile.completedAchievementCount());
            case "achievements_total" -> String.valueOf(plugin.getAchievementConfigLoader().getAll().size());
            case "badges_count" -> String.valueOf(profile.getBadges().size());
            case "login_streak" -> String.valueOf(profile.getLoginStreak());
            case "playtime_hours" -> String.valueOf(profile.getPlaytimeSeconds() / 3600);
            case "playtime_formatted" -> TimeUtil.format(profile.getPlaytimeSeconds());
            default -> null;
        };
    }
}
