package com.dogetennant.dplayerprofiles.player;

import com.dogetennant.dplayerprofiles.DPlayerProfiles;
import com.dogetennant.dplayerprofiles.database.DatabaseManager;
import com.dogetennant.dplayerprofiles.model.PlayerProfile;
import com.dogetennant.dplayerprofiles.util.LogUtil;
import com.dogetennant.dplayerprofiles.util.TimeUtil;
import com.dogetennant.dplayerprofiles.util.VanishUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class ProfileManager implements Listener {

    private final DPlayerProfiles plugin;
    private final DatabaseManager db;
    private final Map<UUID, PlayerProfile> cache = new ConcurrentHashMap<>();
    /** Online players whose "last seen" is frozen because they are vanished. */
    private final Set<UUID> hiddenOnline = ConcurrentHashMap.newKeySet();
    /** Joined vanished: the day's login (streak, login achievements) counts once they reappear. */
    private final Set<UUID> deferredLogins = ConcurrentHashMap.newKeySet();

    public ProfileManager(DPlayerProfiles plugin, DatabaseManager db) {
        this.plugin = plugin;
        this.db = db;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onLogin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        // Joining vanished: to everyone else they are still offline, so "last seen" stays put
        boolean hidden = isHidden(player);
        if (hidden) hiddenOnline.add(uuid); else hiddenOnline.remove(uuid);
        // ...and the login itself (streak, login date, login achievements) waits for them to reappear
        boolean deferLogin = isLoginHidden(player);
        deferredLogins.remove(uuid);

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                PlayerProfile profile = db.loadPlayer(uuid);
                boolean isNew = profile == null;
                if (profile == null) {
                    // New player
                    profile = new PlayerProfile(uuid, player.getName(), now, now, 0, 1, TimeUtil.today());
                    db.upsertPlayer(uuid, player.getName(), now, now, 0, 1, TimeUtil.today());
                } else if (deferLogin) {
                    // Counted when they reappear (completeDeferredLogin); nothing written now
                    profile.setUsername(player.getName());
                } else {
                    // Update streak
                    updateStreak(profile, now);
                    // Update username in case it changed
                    profile.setUsername(player.getName());
                    if (!hidden) profile.setLastSeen(now);
                    db.updateStreak(uuid, profile.getLoginStreak(), profile.getLastLoginDate(), profile.getLastSeen());
                }
                cache.put(uuid, profile);
                boolean deferred = deferLogin && !isNew;

                // Fire login triggers on main thread after profile is loaded
                PlayerProfile finalProfile = profile;
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (deferred) {
                        deferredLogins.add(uuid);
                        // reappeared while the profile was still loading
                        if (player.isOnline() && !isLoginHidden(player)) completeDeferredLogin(player);
                        return;
                    }
                    plugin.getAchievementManager().onLogin(player, finalProfile);
                });
            } catch (SQLException e) {
                LogUtil.severe("Failed to load profile for " + player.getName(), e);
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        boolean hidden = hiddenOnline.remove(uuid) | isHidden(event.getPlayer());
        deferredLogins.remove(uuid);
        PlayerProfile profile = cache.remove(uuid);
        if (profile == null) return;

        // Leaving while vanished: they already "left" when they vanished, keep that time
        if (!hidden) profile.setLastSeen(System.currentTimeMillis());
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                db.upsertPlayer(uuid, profile.getUsername(), profile.getFirstSeen(),
                        profile.getLastSeen(), profile.getPlaytimeSeconds(),
                        profile.getLoginStreak(), profile.getLastLoginDate());
            } catch (SQLException e) {
                LogUtil.severe("Failed to save profile for " + profile.getUsername(), e);
            }
        });
    }

    private boolean isHidden(Player player) {
        return plugin.getConfigManager().get().lastSeenIgnoreVanished && VanishUtil.isVanished(player);
    }

    /** Watches for players vanishing and reappearing mid-session (once a second). */
    public void startVanishWatch() {
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::checkVanishTransitions, 20L, 20L);
    }

    private void checkVanishTransitions() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            boolean hidden = isHidden(player);
            if (hidden && hiddenOnline.add(uuid)) {
                // Vanishing looks like logging off: last seen = now, then frozen
                touchLastSeen(uuid);
            } else if (!hidden && hiddenOnline.remove(uuid)) {
                // Reappearing looks like joining
                touchLastSeen(uuid);
            }
            if (deferredLogins.contains(uuid) && !isLoginHidden(player)) {
                completeDeferredLogin(player);
            }
        }
    }

    private boolean isLoginHidden(Player player) {
        return plugin.getConfigManager().get().loginIgnoreVanished && VanishUtil.isVanished(player);
    }

    /** A player who joined vanished has reappeared: count today's login now, as a join would. */
    private void completeDeferredLogin(Player player) {
        UUID uuid = player.getUniqueId();
        PlayerProfile profile = cache.get(uuid);
        if (profile == null || !deferredLogins.remove(uuid)) return;
        long now = System.currentTimeMillis();
        updateStreak(profile, now);
        profile.setLastSeen(now);
        int streak = profile.getLoginStreak();
        String date = profile.getLastLoginDate();
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                db.updateStreak(uuid, streak, date, now);
            } catch (SQLException e) {
                LogUtil.severe("Failed to save the login of " + player.getName(), e);
            }
        });
        plugin.getAchievementManager().onLogin(player, profile);
    }

    private void touchLastSeen(UUID uuid) {
        PlayerProfile profile = cache.get(uuid);
        if (profile == null) return;
        long now = System.currentTimeMillis();
        profile.setLastSeen(now);
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                db.updateLastSeen(uuid, now);
            } catch (SQLException e) {
                LogUtil.severe("Failed to save last seen for " + uuid, e);
            }
        });
    }

    private void updateStreak(PlayerProfile profile, long now) {
        String lastDate = profile.getLastLoginDate();
        String today = TimeUtil.today();
        if (lastDate == null) {
            profile.setLoginStreak(1);
            profile.setLastLoginDate(today);
        } else if (TimeUtil.isToday(lastDate)) {
            // Already logged in today - no change
        } else if (TimeUtil.isYesterday(lastDate)) {
            profile.setLoginStreak(profile.getLoginStreak() + 1);
            profile.setLastLoginDate(today);
        } else {
            // Streak broken
            profile.setLoginStreak(1);
            profile.setLastLoginDate(today);
        }
    }

    public PlayerProfile get(UUID uuid) {
        return cache.get(uuid);
    }

    public boolean isLoaded(UUID uuid) {
        return cache.containsKey(uuid);
    }

    /**
     * Loads a profile by UUID. Checks the online cache first, then the database.
     * Callback is always called on the main thread with the profile, or null if not found.
     */
    public void loadProfileByUUIDAsync(UUID uuid, Consumer<PlayerProfile> callback) {
        PlayerProfile cached = cache.get(uuid);
        if (cached != null) {
            plugin.getServer().getScheduler().runTask(plugin, () -> callback.accept(cached));
            return;
        }
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                PlayerProfile loaded = db.loadPlayer(uuid);
                plugin.getServer().getScheduler().runTask(plugin, () -> callback.accept(loaded));
            } catch (SQLException e) {
                LogUtil.severe("Failed to load profile for UUID: " + uuid, e);
                plugin.getServer().getScheduler().runTask(plugin, () -> callback.accept(null));
            }
        });
    }

    /**
     * Loads a profile by case-insensitive name. Checks the online cache first, then the database.
     * Callback is always called on the main thread with the profile, or null if not found.
     */
    public void loadProfileByNameAsync(String name, Consumer<PlayerProfile> callback) {
        for (PlayerProfile cached : cache.values()) {
            if (cached.getUsername().equalsIgnoreCase(name)) {
                plugin.getServer().getScheduler().runTask(plugin, () -> callback.accept(cached));
                return;
            }
        }
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                PlayerProfile loaded = db.loadPlayerByName(name);
                plugin.getServer().getScheduler().runTask(plugin, () -> callback.accept(loaded));
            } catch (SQLException e) {
                LogUtil.severe("Failed to load profile for name: " + name, e);
                plugin.getServer().getScheduler().runTask(plugin, () -> callback.accept(null));
            }
        });
    }

    public void setPinnedBadges(UUID uuid, List<String> pinned) {
        PlayerProfile profile = cache.get(uuid);
        if (profile != null) profile.setPinnedBadges(pinned);
        String encoded = String.join(",", pinned);
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                db.setPinnedBadges(uuid, encoded);
            } catch (SQLException e) {
                LogUtil.severe("Failed to save pinned badges for " + uuid, e);
            }
        });
    }

    public void setPrivacy(UUID uuid, boolean isPrivate) {
        PlayerProfile profile = cache.get(uuid);
        if (profile != null) profile.setPrivate(isPrivate);
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                db.setProfilePrivacy(uuid, isPrivate);
            } catch (SQLException e) {
                LogUtil.severe("Failed to save privacy for " + uuid, e);
            }
        });
    }

    public void flushPlaytime(UUID uuid) {
        PlayerProfile profile = cache.get(uuid);
        if (profile == null) return;
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                db.updatePlaytime(uuid, profile.getPlaytimeSeconds());
            } catch (SQLException e) {
                LogUtil.severe("Failed to flush playtime for " + uuid, e);
            }
        });
    }

    public void saveProgress(UUID uuid, String achievementId, long progress, long completedAt) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                db.upsertAchievementProgress(uuid, achievementId, progress, completedAt);
            } catch (SQLException e) {
                LogUtil.severe("Failed to save achievement progress for " + uuid, e);
            }
        });
    }

    public void saveBadge(UUID uuid, String badgeId, long grantedAt, String grantedBy) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                db.insertBadge(uuid, badgeId, grantedAt, grantedBy);
            } catch (SQLException e) {
                LogUtil.severe("Failed to save badge for " + uuid, e);
            }
        });
    }

    public void removeBadge(UUID uuid, String badgeId) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                db.deleteBadge(uuid, badgeId);
            } catch (SQLException e) {
                LogUtil.severe("Failed to remove badge for " + uuid, e);
            }
        });
    }

    public void resetPlayer(UUID uuid) throws SQLException {
        cache.remove(uuid);
        db.deletePlayer(uuid);
    }

    public void resetAchievement(UUID uuid, String achievementId) throws SQLException {
        PlayerProfile profile = cache.get(uuid);
        if (profile != null) {
            profile.getAchievements().remove(achievementId);
        }
        db.deleteAchievementProgress(uuid, achievementId);
    }
}
