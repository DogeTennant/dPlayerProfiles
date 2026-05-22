package com.dogetennant.dplayerprofiles.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class PlayerProfile {

    private final UUID uuid;
    private String username;
    private long firstSeen;
    private long lastSeen;

    private long playtimeSeconds;
    private int loginStreak;
    private String lastLoginDate; // ISO yyyy-MM-dd
    private boolean isPrivate = false;

    // achievement_id -> progress (long). completedAt == 0 means not completed.
    private final Map<String, AchievementProgress> achievements = new HashMap<>();

    // badge_id -> grantedAt epoch ms
    private final Map<String, Long> badges = new HashMap<>();

    // ordered list of up to 3 badge IDs to show in chat hover (empty = show first 3 earned)
    private List<String> pinnedBadges = new ArrayList<>();

    public PlayerProfile(UUID uuid, String username, long firstSeen, long lastSeen,
                         long playtimeSeconds, int loginStreak, String lastLoginDate) {
        this.uuid = uuid;
        this.username = username;
        this.firstSeen = firstSeen;
        this.lastSeen = lastSeen;
        this.playtimeSeconds = playtimeSeconds;
        this.loginStreak = loginStreak;
        this.lastLoginDate = lastLoginDate;
    }

    public UUID getUuid() { return uuid; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public long getFirstSeen() { return firstSeen; }
    public long getLastSeen() { return lastSeen; }
    public void setLastSeen(long lastSeen) { this.lastSeen = lastSeen; }
    public long getPlaytimeSeconds() { return playtimeSeconds; }
    public void addPlaytime(long seconds) { this.playtimeSeconds += seconds; }
    public int getLoginStreak() { return loginStreak; }
    public void setLoginStreak(int loginStreak) { this.loginStreak = loginStreak; }
    public String getLastLoginDate() { return lastLoginDate; }
    public void setLastLoginDate(String lastLoginDate) { this.lastLoginDate = lastLoginDate; }

    public Map<String, AchievementProgress> getAchievements() { return achievements; }
    public Map<String, Long> getBadges() { return badges; }
    public List<String> getPinnedBadges() { return pinnedBadges; }
    public void setPinnedBadges(List<String> pinnedBadges) { this.pinnedBadges = new ArrayList<>(pinnedBadges); }

    /** Badge IDs to show in the chat hover: pinned selection if set, otherwise first 3 by earn order. */
    public List<String> getEffectiveChatBadges() {
        if (!pinnedBadges.isEmpty()) {
            return pinnedBadges.stream().filter(badges::containsKey).limit(3).collect(Collectors.toList());
        }
        return badges.entrySet().stream()
                .sorted(Comparator.comparingLong(Map.Entry::getValue))
                .limit(3)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    public long getProgress(String achievementId) {
        AchievementProgress p = achievements.get(achievementId);
        return p == null ? 0L : p.progress();
    }

    public boolean isCompleted(String achievementId) {
        AchievementProgress p = achievements.get(achievementId);
        return p != null && p.completedAt() > 0;
    }

    public void setProgress(String achievementId, long progress, long completedAt) {
        achievements.put(achievementId, new AchievementProgress(progress, completedAt));
    }

    public boolean isPrivate() { return isPrivate; }
    public void setPrivate(boolean isPrivate) { this.isPrivate = isPrivate; }

    public boolean hasBadge(String badgeId) { return badges.containsKey(badgeId.toLowerCase()); }

    public long grantBadge(String badgeId, long grantedAt) {
        badges.put(badgeId.toLowerCase(), grantedAt);
        return grantedAt;
    }

    public boolean revokeBadge(String badgeId) {
        return badges.remove(badgeId.toLowerCase()) != null;
    }

    public int completedAchievementCount() {
        return (int) achievements.values().stream().filter(p -> p.completedAt() > 0).count();
    }

    public record AchievementProgress(long progress, long completedAt) {}
}
