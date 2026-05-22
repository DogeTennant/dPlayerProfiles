package com.dogetennant.dplayerprofiles.integration;

import com.DogeTennant.oneinthechamberreborn.api.events.OITCGameWinEvent;
import com.DogeTennant.oneinthechamberreborn.api.events.OITCKillEvent;
import com.dogetennant.dplayerprofiles.DPlayerProfiles;
import com.dogetennant.dplayerprofiles.model.TriggerType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class OITCHook implements Listener {

    private final DPlayerProfiles plugin;

    public OITCHook(DPlayerProfiles plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onKill(OITCKillEvent e) {
        plugin.getAchievementManager().increment(
                e.getKiller(),
                TriggerType.OITC_KILL,
                e.getArena().getName(),
                1);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWin(OITCGameWinEvent e) {
        plugin.getAchievementManager().increment(
                e.getWinner(),
                TriggerType.OITC_WIN,
                e.getArena().getName(),
                1);
    }
}
