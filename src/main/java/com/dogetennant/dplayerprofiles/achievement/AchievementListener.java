package com.dogetennant.dplayerprofiles.achievement;

import com.dogetennant.dplayerprofiles.DPlayerProfiles;
import com.dogetennant.dplayerprofiles.config.AchievementConfigLoader;
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
import org.bukkit.inventory.ItemStack;

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
        Block block = event.getBlock();
        String material = block.getType().name();

        if (!plugin.getAchievementConfigLoader().isWatched(TriggerType.BLOCK_BREAK, material)) return;
        // Breaking a block a player put there is farming, not progress.
        if (plugin.getPlacedBlockTracker().isPlayerPlaced(block)) return;

        plugin.getAchievementManager().increment(event.getPlayer(), TriggerType.BLOCK_BREAK, material, 1);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlock();
        String material = block.getType().name();

        AchievementConfigLoader configLoader = plugin.getAchievementConfigLoader();
        if (!configLoader.isBlockMaterialWatched(material)) return;

        // Read before marking: a position already used once never pays out again,
        // which is what caps the place/break loop.
        PlacedBlockTracker tracker = plugin.getPlacedBlockTracker();
        boolean alreadyUsed = tracker.isPlayerPlaced(block);
        tracker.markPlayerPlaced(block, material);
        if (alreadyUsed) return;

        plugin.getAchievementManager().increment(event.getPlayer(), TriggerType.BLOCK_PLACE, material, 1);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;
        plugin.getAchievementManager().increment(event.getPlayer(), TriggerType.FISH, null, 1);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        // the result slot holds what is crafted; complex recipes (dyed armour, fireworks, ...)
        // report an empty result through getRecipe()
        ItemStack result = event.getInventory().getResult();
        if (result == null) result = event.getRecipe().getResult();
        String material = result.getType().name();
        plugin.getAchievementManager().increment(player, TriggerType.CRAFT, material, 1);
    }
}
