package com.dogetennant.dplayerprofiles.reward;

import com.dogetennant.dplayerprofiles.Fakes;
import com.dogetennant.dplayerprofiles.lang.LanguageManager;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** What an achievement reward gives the player. */
class RewardManagerTest {

    private final Server server = Fakes.server();
    private final ConsoleCommandSender console = mock(ConsoleCommandSender.class);
    private final Player alex = mock(Player.class);
    private final PlayerInventory inventory = mock(PlayerInventory.class);
    private final World world = mock(World.class);
    private final Location feet = new Location(world, 3, 64, 7);
    private RewardManager rewards;

    @BeforeEach
    void setUp() {
        Fakes.plugin(Fakes.config());
        when(server.getConsoleSender()).thenReturn(console);
        when(alex.getName()).thenReturn("Alex");
        when(alex.getInventory()).thenReturn(inventory);
        when(alex.getWorld()).thenReturn(world);
        when(alex.getLocation()).thenReturn(feet);
        rewards = new RewardManager(mock(LanguageManager.class));
    }

    /** What the player is given: a copy of the stored reward, which is a copy of the template. */
    private final ItemStack given = mock(ItemStack.class);

    private RewardEntry itemReward() {
        ItemStack stored = mock(ItemStack.class);
        when(stored.getType()).thenReturn(org.bukkit.Material.DIAMOND);
        when(stored.clone()).thenReturn(given);
        ItemStack template = mock(ItemStack.class);
        when(template.getType()).thenReturn(org.bukkit.Material.DIAMOND);
        when(template.getAmount()).thenReturn(3);
        when(template.clone()).thenReturn(stored);
        return RewardEntry.item(template);
    }

    @Test
    void aCommandRunsFromTheConsoleWithThePlayersName() {
        rewards.grant(alex, List.of(RewardEntry.command("eco give {player} 100")));

        verify(server).dispatchCommand(console, "eco give Alex 100");
    }

    @Test
    void anItemGoesIntoTheInventory() {
        when(inventory.addItem(any(ItemStack[].class))).thenReturn(new HashMap<>());

        rewards.grant(alex, List.of(itemReward()));

        verify(inventory).addItem(given);
        verify(world, never()).dropItemNaturally(any(), any());
    }

    @Test
    void whatDoesNotFitIntoAFullInventoryIsDroppedAtThePlayersFeet() {
        ItemStack leftover = mock(ItemStack.class);
        when(inventory.addItem(any(ItemStack[].class))).thenReturn(new HashMap<>(Map.of(0, leftover)));

        rewards.grant(alex, List.of(itemReward()));

        verify(world).dropItemNaturally(feet, leftover);
    }

    @Test
    void aBadgeIsHandedToTheBadgeGranter() {
        @SuppressWarnings("unchecked")
        BiConsumer<Player, String> badges = mock(BiConsumer.class);
        rewards.setBadgeGranter(badges);

        rewards.grant(alex, List.of(RewardEntry.badge("angler")));

        verify(badges).accept(alex, "angler");
    }
}
