package com.dogetennant.dplayerprofiles.integration;

import com.dogetennant.dplayerprofiles.DPlayerProfiles;
import com.dogetennant.dplayerprofiles.model.TriggerType;
import com.magmaguy.elitemobs.api.ArenaCompleteEvent;
import com.magmaguy.elitemobs.api.DungeonCompleteEvent;
import com.magmaguy.elitemobs.api.EliteMobDeathEvent;
import com.magmaguy.elitemobs.api.QuestCompleteEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class EliteMobsHook implements Listener {

    private final DPlayerProfiles plugin;

    public EliteMobsHook(DPlayerProfiles plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEliteDeath(EliteMobDeathEvent e) {
        if (e.getEntityDeathEvent() == null) return; // Ender Dragon edge case
        Player killer = e.getEntityDeathEvent().getEntity().getKiller();
        if (killer == null) return;
        String entityType = e.getEliteEntity().getLivingEntity().getType().name();
        plugin.getAchievementManager().increment(killer, TriggerType.ELITEMOBS_KILL, entityType, 1);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDungeonComplete(DungeonCompleteEvent e) {
        String name = e.getDungeonInstance().getContentPackagesConfigFields().getName();
        for (Player player : e.getDungeonInstance().getParticipants()) {
            plugin.getAchievementManager().increment(player, TriggerType.ELITEMOBS_DUNGEON_COMPLETE, name, 1);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onArenaComplete(ArenaCompleteEvent e) {
        String name = e.getArenaInstance().getCustomArenasConfigFields().getArenaName();
        for (Player player : e.getArenaInstance().getParticipants()) {
            plugin.getAchievementManager().increment(player, TriggerType.ELITEMOBS_ARENA_COMPLETE, name, 1);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onQuestComplete(QuestCompleteEvent e) {
        plugin.getAchievementManager().increment(
                e.getPlayer(), TriggerType.ELITEMOBS_QUEST_COMPLETE, e.getQuest().getQuestName(), 1);
    }
}
