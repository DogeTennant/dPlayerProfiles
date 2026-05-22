package com.dogetennant.dplayerprofiles.integration;

import com.dogetennant.dplayerprofiles.DPlayerProfiles;
import com.dogetennant.dplayerprofiles.model.TriggerType;
import com.meteordevelopments.duels.api.event.match.MatchEndEvent;
import com.meteordevelopments.duels.api.kit.Kit;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class DuelsHook implements Listener {

    private final DPlayerProfiles plugin;

    public DuelsHook(DPlayerProfiles plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onMatchEnd(MatchEndEvent e) {
        if (e.getWinner() == null) return; // TIE or no winner

        Player winner = Bukkit.getPlayer(e.getWinner());
        if (winner == null) return; // winner went offline

        Kit kit = e.getMatch().getKit();
        String kitName = kit != null ? kit.getName() : "*";

        plugin.getAchievementManager().increment(winner, TriggerType.DUEL_WIN, kitName, 1);

        if (e.getReason() == MatchEndEvent.Reason.OPPONENT_DEFEAT) {
            plugin.getAchievementManager().increment(winner, TriggerType.DUEL_KILL, kitName, 1);
        }
    }
}
