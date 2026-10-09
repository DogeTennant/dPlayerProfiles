package com.dogetennant.dplayerprofiles.player;

import com.dogetennant.dplayerprofiles.DPlayerProfiles;
import com.dogetennant.dplayerprofiles.database.DatabaseManager;
import com.dogetennant.dplayerprofiles.database.DatabaseManager.PendingTrigger;
import com.dogetennant.dplayerprofiles.database.DatabaseQueue;
import com.dogetennant.dplayerprofiles.model.PlayerProfile;
import com.dogetennant.dplayerprofiles.model.TriggerType;
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

/**
 * Profiles of online players, kept in memory. Every database read and write goes through the
 * plugin's {@link DatabaseQueue}, one after another in the order they were made: a completion is
 * never overwritten by the progress saved just before it, and a login loads what the quit saved.
 */
public class ProfileManager implements Listener {

    private final DPlayerProfiles plugin;
    private final DatabaseManager db;
    private final Map<UUID, PlayerProfile> cache = new ConcurrentHashMap<>();
    /** Online players whose "last seen" is frozen because they are vanished. */
    private final Set<UUID> hiddenOnline = ConcurrentHashMap.newKeySet();
    /** Joined vanished: the day's login (streak, login achievements) counts once they reappear. */
    private final Set<UUID> deferredLogins = ConcurrentHashMap.newKeySet();
    /** Triggers from while they were offline, counted with the deferred login. */
    private final Map<UUID, List<PendingTrigger>> deferredTriggers = new ConcurrentHashMap<>();

    public ProfileManager(DPlayerProfiles plugin, DatabaseManager db) {
        this.plugin = plugin;
        this.db = db;
    }

    private DatabaseQueue queue() {
        return plugin.getDatabaseQueue();
    }

    /** What a login read from the database. */
    private record Loaded(PlayerProfile profile, boolean isNew, List<PendingTrigger> pending) {}

    @EventHandler(priority = EventPriority.MONITOR)
    public void onLogin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        String name = player.getName();
        long now = System.currentTimeMillis();
        // Joining vanished: to everyone else they are still offline, so "last seen" stays put
        boolean hidden = isHidden(player);
        if (hidden) hiddenOnline.add(uuid); else hiddenOnline.remove(uuid);
        // ...and the login itself (streak, login date, login achievements) waits for them to reappear
        boolean deferLogin = isLoginHidden(player);
        deferredLogins.remove(uuid);
        deferredTriggers.remove(uuid);

        queue().query(() -> {
            try {
                PlayerProfile profile = db.loadPlayer(uuid);
                boolean isNew = profile == null;
                if (profile == null) {
                    // New player
                    profile = new PlayerProfile(uuid, name, now, now, 0, 1, TimeUtil.today());
                    db.upsertPlayer(uuid, name, now, now, 0, 1, TimeUtil.today());
                } else if (deferLogin) {
                    // Counted when they reappear (completeDeferredLogin); nothing written now
                    profile.setUsername(name);
                } else {
                    updateStreak(profile, now);
                    // Update username in case it changed
                    profile.setUsername(name);
                    if (!hidden) profile.setLastSeen(now);
                    db.updateStreak(uuid, profile.getLoginStreak(), profile.getLastLoginDate(), profile.getLastSeen());
                }
                return new Loaded(profile, isNew, db.getPendingTriggers(uuid));
            } catch (SQLException e) {
                LogUtil.severe("Failed to load profile for " + name, e);
                return null;
            }
        }, loaded -> {
            // left again before the profile was loaded: do not keep it
            if (loaded == null || !player.isOnline()) return;
            cache.put(uuid, loaded.profile());
            if (deferLogin && !loaded.isNew()) {
                deferredLogins.add(uuid);
                deferredTriggers.put(uuid, loaded.pending());
                // reappeared while the profile was still loading
                if (!isLoginHidden(player)) completeDeferredLogin(player);
                return;
            }
            plugin.getAchievementManager().onLogin(player, loaded.profile());
            applyPendingTriggers(player, loaded.pending());
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        boolean hidden = hiddenOnline.remove(uuid) | isHidden(event.getPlayer());
        deferredLogins.remove(uuid);
        deferredTriggers.remove(uuid);
        PlayerProfile profile = cache.remove(uuid);
        if (profile == null) return;

        // Leaving while vanished: they already "left" when they vanished, keep that time
        if (!hidden) profile.setLastSeen(System.currentTimeMillis());
        saveProfile(profile);
    }

    /** Saves a profile's own row (name, times, playtime, streak). */
    private void saveProfile(PlayerProfile profile) {
        UUID uuid = profile.getUuid();
        String username = profile.getUsername();
        long firstSeen = profile.getFirstSeen(), lastSeen = profile.getLastSeen();
        long playtime = profile.getPlaytimeSeconds();
        int streak = profile.getLoginStreak();
        String lastLogin = profile.getLastLoginDate();
        write("save profile for " + username, () ->
                db.upsertPlayer(uuid, username, firstSeen, lastSeen, playtime, streak, lastLogin));
    }

    /**
     * Saves every online player's profile (the server is stopping: plugins are disabled before the
     * players are kicked, so their quit is never seen). Call before closing the database queue.
     */
    public void saveAll() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            PlayerProfile profile = cache.get(player.getUniqueId());
            if (profile == null) continue;
            if (!hiddenOnline.contains(player.getUniqueId())) profile.setLastSeen(System.currentTimeMillis());
            saveProfile(profile);
        }
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
        write("save the login of " + player.getName(), () -> db.updateStreak(uuid, streak, date, now));
        plugin.getAchievementManager().onLogin(player, profile);
        List<PendingTrigger> pending = deferredTriggers.remove(uuid);
        if (pending != null) applyPendingTriggers(player, pending);
    }

    /** Counts what happened while the player was offline, then forgets it. */
    private void applyPendingTriggers(Player player, List<PendingTrigger> pending) {
        if (pending.isEmpty()) return;
        for (PendingTrigger trigger : pending) {
            TriggerType type;
            try {
                type = TriggerType.valueOf(trigger.triggerType());
            } catch (IllegalArgumentException e) {
                continue; // a trigger type this version no longer has
            }
            plugin.getAchievementManager().increment(player, type, trigger.target(), trigger.amount());
        }
        List<Long> ids = pending.stream().map(PendingTrigger::id).toList();
        write("forget counted offline triggers", () -> db.deletePendingTriggers(ids));
    }

    /** Keeps a trigger for an offline player (e.g. a tournament won after logging off) for their next login. */
    public void addPendingTrigger(UUID uuid, TriggerType type, String target, long amount) {
        long now = System.currentTimeMillis();
        write("keep an offline trigger", () -> db.addPendingTrigger(uuid, type.name(), target, amount, now));
    }

    private void touchLastSeen(UUID uuid) {
        PlayerProfile profile = cache.get(uuid);
        if (profile == null) return;
        long now = System.currentTimeMillis();
        profile.setLastSeen(now);
        write("save last seen for " + uuid, () -> db.updateLastSeen(uuid, now));
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
        queue().query(() -> read("load profile for UUID " + uuid, () -> db.loadPlayer(uuid)), callback);
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
        queue().query(() -> read("load profile for name " + name, () -> db.loadPlayerByName(name)), callback);
    }

    public void setPinnedBadges(UUID uuid, List<String> pinned) {
        PlayerProfile profile = cache.get(uuid);
        if (profile != null) profile.setPinnedBadges(pinned);
        String encoded = String.join(",", pinned);
        write("save pinned badges for " + uuid, () -> db.setPinnedBadges(uuid, encoded));
    }

    public void setPrivacy(UUID uuid, boolean isPrivate) {
        PlayerProfile profile = cache.get(uuid);
        if (profile != null) profile.setPrivate(isPrivate);
        write("save privacy for " + uuid, () -> db.setProfilePrivacy(uuid, isPrivate));
    }

    public void flushPlaytime(UUID uuid) {
        PlayerProfile profile = cache.get(uuid);
        if (profile == null) return;
        long playtime = profile.getPlaytimeSeconds();
        write("flush playtime for " + uuid, () -> db.updatePlaytime(uuid, playtime));
    }

    public void saveProgress(UUID uuid, String achievementId, long progress, long completedAt) {
        write("save achievement progress for " + uuid,
                () -> db.upsertAchievementProgress(uuid, achievementId, progress, completedAt));
    }

    public void saveBadge(UUID uuid, String badgeId, long grantedAt, String grantedBy) {
        write("save badge for " + uuid, () -> db.insertBadge(uuid, badgeId, grantedAt, grantedBy));
    }

    public void removeBadge(UUID uuid, String badgeId) {
        write("remove badge for " + uuid, () -> db.deleteBadge(uuid, badgeId));
    }

    /** Deletes everything stored about the player; {@code done} gets whether it worked (main thread). */
    public void resetPlayer(UUID uuid, Consumer<Boolean> done) {
        cache.remove(uuid);
        queue().query(() -> read("reset player " + uuid, () -> {
            db.deletePlayer(uuid);
            return true;
        }) != null, done);
    }

    /** Removes one achievement's progress; {@code done} gets whether it worked (main thread). */
    public void resetAchievement(UUID uuid, String achievementId, Consumer<Boolean> done) {
        PlayerProfile profile = cache.get(uuid);
        if (profile != null) {
            profile.getAchievements().remove(achievementId);
        }
        queue().query(() -> read("reset achievement " + achievementId + " of " + uuid, () -> {
            db.deleteAchievementProgress(uuid, achievementId);
            return true;
        }) != null, done);
    }

    private interface SqlWrite {
        void run() throws SQLException;
    }

    private interface SqlRead<T> {
        T get() throws SQLException;
    }

    /** Queues a write; a failure is logged as "Failed to {@code what}". */
    private void write(String what, SqlWrite write) {
        queue().submit(() -> {
            try {
                write.run();
            } catch (SQLException e) {
                LogUtil.severe("Failed to " + what, e);
            }
        });
    }

    /** Runs a read on the database thread; null after a failure (logged as "Failed to {@code what}"). */
    private static <T> T read(String what, SqlRead<T> read) {
        try {
            return read.get();
        } catch (SQLException e) {
            LogUtil.severe("Failed to " + what, e);
            return null;
        }
    }
}
