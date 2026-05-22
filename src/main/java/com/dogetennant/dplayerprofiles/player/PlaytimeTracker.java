package com.dogetennant.dplayerprofiles.player;

import com.dogetennant.dplayerprofiles.DPlayerProfiles;
import com.dogetennant.dplayerprofiles.model.PlayerProfile;
import com.dogetennant.dplayerprofiles.model.TriggerType;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlaytimeTracker implements Listener {

    private final DPlayerProfiles plugin;
    private BukkitTask task;

    // Per-player AFK detection state
    private final Map<UUID, float[]> lastRotation = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> stillSeconds  = new ConcurrentHashMap<>();
    // Players who sent a chat message since the last tick (resets their AFK timer)
    private final Set<UUID> activeChatters = ConcurrentHashMap.newKeySet();

    public PlaytimeTracker(DPlayerProfiles plugin) {
        this.plugin = plugin;
    }

    public void start(int intervalSeconds) {
        long intervalTicks = (long) intervalSeconds * 20;
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, intervalTicks, intervalTicks);
    }

    public void shutdown() {
        if (task != null) task.cancel();
    }

    private void tick() {
        ProfileManager profileManager = plugin.getProfileManager();
        int elapsed = plugin.getConfigManager().get().playtimeUpdateInterval;
        boolean antiAfkEnabled = plugin.getConfigManager().get().antiAfkEnabled;
        int afkTimeout = plugin.getConfigManager().get().antiAfkTimeout;

        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            PlayerProfile profile = profileManager.get(uuid);
            if (profile == null) continue;

            if (antiAfkEnabled && isAfk(player, uuid, elapsed, afkTimeout)) continue;

            profile.addPlaytime(elapsed);
            plugin.getAchievementManager().increment(player, TriggerType.TIME_PLAYED, null, elapsed);
            profileManager.flushPlaytime(uuid);
        }
    }

    /**
     * Returns true if the player should be considered AFK this tick.
     * Accumulates still-seconds when camera hasn't moved; chatting resets it.
     */
    private boolean isAfk(Player player, UUID uuid, int elapsed, int afkTimeout) {
        float yaw   = player.getLocation().getYaw();
        float pitch = player.getLocation().getPitch();
        float[] last = lastRotation.get(uuid);
        boolean chatted = activeChatters.remove(uuid);

        boolean active = chatted || last == null || last[0] != yaw || last[1] != pitch;
        lastRotation.put(uuid, new float[]{yaw, pitch});

        if (active) {
            stillSeconds.remove(uuid);
            return false;
        } else {
            int still = stillSeconds.getOrDefault(uuid, 0) + elapsed;
            stillSeconds.put(uuid, still);
            return still >= afkTimeout;
        }
    }

    //  Event listeners 

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        // Runs async - ConcurrentHashSet is safe here
        activeChatters.add(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        lastRotation.remove(uuid);
        stillSeconds.remove(uuid);
        activeChatters.remove(uuid);
    }
}
