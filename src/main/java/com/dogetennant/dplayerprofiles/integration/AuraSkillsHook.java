package com.dogetennant.dplayerprofiles.integration;

import com.dogetennant.dplayerprofiles.DPlayerProfiles;
import com.dogetennant.dplayerprofiles.model.TriggerType;
import dev.aurelium.auraskills.api.event.skill.SkillLevelUpEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class AuraSkillsHook implements Listener {

    private final DPlayerProfiles plugin;

    public AuraSkillsHook(DPlayerProfiles plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLevelUp(SkillLevelUpEvent e) {
        // skill key is lowercase, e.g. "mining", "fighting" - matches target in achievement config
        String skillKey = e.getSkill().getId().getKey();
        plugin.getAchievementManager().increment(
                e.getPlayer(), TriggerType.AURASKILLS_LEVEL_UP, skillKey, e.getLevel());
    }
}
