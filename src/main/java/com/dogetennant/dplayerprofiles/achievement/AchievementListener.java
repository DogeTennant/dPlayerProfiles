package com.dogetennant.dplayerprofiles.achievement;

import com.dogetennant.dplayerprofiles.DPlayerProfiles;
import com.dogetennant.dplayerprofiles.integration.CoreProtectHook;
import com.dogetennant.dplayerprofiles.model.TriggerType;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerFishEvent;

public class AchievementListener implements Listener {

    private final DPlayerProfiles plugin;

    public AchievementListener(DPlayerProfiles plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;

        if (event.getEntity() instanceof Player) {
            plugin.getAchievementManager().increment(killer, TriggerType.PLAYER_KILL, null, 1);
        } else {
            String entityType = event.getEntityType().name();
            plugin.getAchievementManager().increment(killer, TriggerType.MOB_KILL, entityType, 1);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        String material = event.getBlock().getType().name();
        Block block = event.getBlock();

        CoreProtectHook cpHook = plugin.getIntegrationManager().getCoreProtectHook();
        if (cpHook == null) {
            plugin.getAchievementManager().increment(player, TriggerType.BLOCK_BREAK, material, 1);
            return;
        }

        // Async CoreProtect lookup: skip if the player placed this block themselves
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            if (!cpHook.wasPlacedBySamePlayer(block, player.getName())) {
                plugin.getServer().getScheduler().runTask(plugin, () ->
                        plugin.getAchievementManager().increment(player, TriggerType.BLOCK_BREAK, material, 1));
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        String material = event.getBlock().getType().name();
        Block block = event.getBlock();

        CoreProtectHook cpHook = plugin.getIntegrationManager().getCoreProtectHook();
        if (cpHook == null) {
            plugin.getAchievementManager().increment(player, TriggerType.BLOCK_PLACE, material, 1);
            return;
        }

        // Async CoreProtect lookup: skip if the player broke this block themselves
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            if (!cpHook.wasBrokenBySamePlayer(block, player.getName())) {
                plugin.getServer().getScheduler().runTask(plugin, () ->
                        plugin.getAchievementManager().increment(player, TriggerType.BLOCK_PLACE, material, 1));
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;
        plugin.getAchievementManager().increment(event.getPlayer(), TriggerType.FISH, null, 1);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String material = event.getRecipe().getResult().getType().name();
        plugin.getAchievementManager().increment(player, TriggerType.CRAFT, material, 1);
    }
}
