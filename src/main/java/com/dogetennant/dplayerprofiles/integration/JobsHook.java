package com.dogetennant.dplayerprofiles.integration;

import com.dogetennant.dplayerprofiles.DPlayerProfiles;
import com.dogetennant.dplayerprofiles.model.TriggerType;
import com.gamingmesh.jobs.api.JobsJoinEvent;
import com.gamingmesh.jobs.api.JobsLevelUpEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class JobsHook implements Listener {

    private final DPlayerProfiles plugin;

    public JobsHook(DPlayerProfiles plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLevelUp(JobsLevelUpEvent e) {
        Player player = e.getPlayer().getPlayer();
        if (player == null) return;
        plugin.getAchievementManager().increment(
                player, TriggerType.JOBS_LEVEL_UP, e.getJob().getName(), e.getLevel());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onJoin(JobsJoinEvent e) {
        Player player = e.getPlayer().getPlayer();
        if (player == null) return;
        plugin.getAchievementManager().increment(
                player, TriggerType.JOBS_JOIN, e.getJob().getName(), 1);
    }
}
