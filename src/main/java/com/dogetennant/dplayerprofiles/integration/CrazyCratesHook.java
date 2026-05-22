package com.dogetennant.dplayerprofiles.integration;

import com.badbones69.crazycrates.paper.api.events.CrateOpenEvent;
import com.dogetennant.dplayerprofiles.DPlayerProfiles;
import com.dogetennant.dplayerprofiles.model.TriggerType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class CrazyCratesHook implements Listener {

    private final DPlayerProfiles plugin;

    public CrazyCratesHook(DPlayerProfiles plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCrateOpen(CrateOpenEvent e) {
        // getFileName() returns the config file name without .yml extension
        String crateName = e.getCrate().getFileName();
        plugin.getAchievementManager().increment(e.getPlayer(), TriggerType.CRATE_OPEN, crateName, 1);
    }
}
