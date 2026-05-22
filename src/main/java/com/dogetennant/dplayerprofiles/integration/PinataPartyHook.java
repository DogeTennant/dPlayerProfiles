package com.dogetennant.dplayerprofiles.integration;

import com.dogetennant.dplayerprofiles.DPlayerProfiles;
import com.dogetennant.dplayerprofiles.model.TriggerType;
import me.hexedhero.pp.api.PinataDieEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class PinataPartyHook implements Listener {

    private final DPlayerProfiles plugin;

    public PinataPartyHook(DPlayerProfiles plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPinataDie(PinataDieEvent e) {
        // getHitters() returns every player who hit the pinata - all get credit for the kill
        for (Object obj : e.getPinata().getHitters()) {
            if (obj instanceof Player player) {
                plugin.getAchievementManager().increment(player, TriggerType.PINATA_KILL, null, 1);
            }
        }
    }
}
