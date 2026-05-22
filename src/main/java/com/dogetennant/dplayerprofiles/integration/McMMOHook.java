package com.dogetennant.dplayerprofiles.integration;

import com.dogetennant.dplayerprofiles.DPlayerProfiles;
import com.dogetennant.dplayerprofiles.model.TriggerType;
import com.gmail.nossr50.events.experience.McMMOPlayerLevelUpEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class McMMOHook implements Listener {

    private final DPlayerProfiles plugin;

    public McMMOHook(DPlayerProfiles plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLevelUp(McMMOPlayerLevelUpEvent e) {
        String skill = e.getSkill().name(); // e.g. "MINING", "SWORDS"
        int newLevel = e.getSkillLevel();
        plugin.getAchievementManager().increment(
                e.getPlayer(), TriggerType.MCMMO_LEVEL_UP, skill, newLevel);
    }
}
