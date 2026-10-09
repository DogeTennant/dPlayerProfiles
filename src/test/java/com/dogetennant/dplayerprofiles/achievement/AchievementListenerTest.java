package com.dogetennant.dplayerprofiles.achievement;

import com.dogetennant.dplayerprofiles.DPlayerProfiles;
import com.dogetennant.dplayerprofiles.Fakes;
import com.dogetennant.dplayerprofiles.config.AchievementConfigLoader;
import com.dogetennant.dplayerprofiles.model.TriggerType;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Which game events count for which achievement trigger. */
class AchievementListenerTest {

    private final DPlayerProfiles plugin = Fakes.plugin(Fakes.config());
    private final AchievementManager achievements = mock(AchievementManager.class);
    private final PlacedBlockTracker placed = mock(PlacedBlockTracker.class);
    private final Player alex = mock(Player.class);
    private AchievementListener listener;

    @BeforeEach
    void setUp() {
        AchievementConfigLoader loader = mock(AchievementConfigLoader.class);
        when(loader.isWatched(any(), any())).thenReturn(true);
        when(loader.isBlockMaterialWatched(any())).thenReturn(true);
        when(plugin.getAchievementConfigLoader()).thenReturn(loader);
        when(plugin.getAchievementManager()).thenReturn(achievements);
        when(plugin.getPlacedBlockTracker()).thenReturn(placed);
        listener = new AchievementListener(plugin);
    }

    private static ItemStack item(Material type) {
        ItemStack item = mock(ItemStack.class);
        when(item.getType()).thenReturn(type);
        return item;
    }

    private CraftItemEvent craft(Material recipeResult, Material resultSlot) {
        Recipe recipe = mock(Recipe.class);
        ItemStack fromRecipe = item(recipeResult);
        when(recipe.getResult()).thenReturn(fromRecipe);
        CraftingInventory inventory = mock(CraftingInventory.class);
        ItemStack inSlot = item(resultSlot);
        when(inventory.getResult()).thenReturn(inSlot);
        CraftItemEvent event = mock(CraftItemEvent.class);
        when(event.getWhoClicked()).thenReturn(alex);
        when(event.getRecipe()).thenReturn(recipe);
        when(event.getInventory()).thenReturn(inventory);
        return event;
    }

    @Test
    void craftingCountsTheCraftedItem() {
        Fakes.fire(listener, craft(Material.TORCH, Material.TORCH));

        verify(achievements).increment(alex, TriggerType.CRAFT, "TORCH", 1);
    }

    @Test
    void aSpecialRecipeCountsWhatComesOutOfTheResultSlot() {
        // dyed leather armour, fireworks, ...: the recipe itself reports an empty result
        Fakes.fire(listener, craft(Material.AIR, Material.LEATHER_CHESTPLATE));

        verify(achievements).increment(alex, TriggerType.CRAFT, "LEATHER_CHESTPLATE", 1);
    }

    @Test
    void breakingABlockAPlayerPlacedDoesNotCount() {
        Block block = mock(Block.class);
        when(block.getType()).thenReturn(Material.DIAMOND_ORE);
        BlockBreakEvent breaking = mock(BlockBreakEvent.class);
        when(breaking.getBlock()).thenReturn(block);
        when(breaking.getPlayer()).thenReturn(alex);

        when(placed.isPlayerPlaced(block)).thenReturn(true);
        Fakes.fire(listener, breaking);
        verify(achievements, never()).increment(any(), any(), any(), anyLong());

        when(placed.isPlayerPlaced(block)).thenReturn(false);
        Fakes.fire(listener, breaking);
        verify(achievements).increment(alex, TriggerType.BLOCK_BREAK, "DIAMOND_ORE", 1);
    }

    @Test
    void killingAMobCountsItsType() {
        Zombie zombie = mock(Zombie.class);
        when(zombie.getKiller()).thenReturn(alex);
        EntityDeathEvent death = mock(EntityDeathEvent.class);
        when(death.getEntity()).thenReturn(zombie);
        when(death.getEntityType()).thenReturn(EntityType.ZOMBIE);

        Fakes.fire(listener, death);

        verify(achievements).increment(alex, TriggerType.MOB_KILL, "ZOMBIE", 1);
    }
}
