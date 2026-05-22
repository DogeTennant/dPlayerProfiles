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
        Player winner = Bukkit.getPlayer(e.getWinner());
        if (winner == null) return; // winner offline - skip; achievements require an online player
        plugin.getAchievementManager().increment(
                winner,
                TriggerType.TOURNAMENT_WIN,
                e.getConfigId(),
                1);
    }
}
