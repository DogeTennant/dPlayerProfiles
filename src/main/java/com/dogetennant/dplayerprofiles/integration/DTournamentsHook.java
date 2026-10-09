package com.dogetennant.dplayerprofiles.integration;

import com.dogetennant.dtournaments.api.events.TournamentEndEvent;
import com.dogetennant.dplayerprofiles.DPlayerProfiles;
import com.dogetennant.dplayerprofiles.model.TriggerType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class DTournamentsHook implements Listener {

    private final DPlayerProfiles plugin;

    public DTournamentsHook(DPlayerProfiles plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onTournamentEnd(TournamentEndEvent e) {
        if (!plugin.getAchievementConfigLoader().isWatched(TriggerType.TOURNAMENT_WIN, e.getConfigId())) return;
        Player winner = Bukkit.getPlayer(e.getWinner());
        if (winner == null || !plugin.getProfileManager().isLoaded(winner.getUniqueId())) {
            // offline (or still loading): counted at their next login
            plugin.getProfileManager().addPendingTrigger(e.getWinner(), TriggerType.TOURNAMENT_WIN, e.getConfigId(), 1);
            return;
        }
        plugin.getAchievementManager().increment(winner, TriggerType.TOURNAMENT_WIN, e.getConfigId(), 1);
    }
}
