package com.dogetennant.dplayerprofiles.gui;

import com.dogetennant.dplayerprofiles.DPlayerProfiles;
import com.dogetennant.dplayerprofiles.util.ColorUtil;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class GuiManager implements Listener {

    private final DPlayerProfiles plugin;
    private final Map<UUID, BaseGui> openGuis = new ConcurrentHashMap<>();
    private final Map<UUID, Consumer<String>> pendingChatInputs = new ConcurrentHashMap<>();

    public GuiManager(DPlayerProfiles plugin) {
        this.plugin = plugin;
    }

    public void open(Player viewer, BaseGui gui) {
        openGuis.put(viewer.getUniqueId(), gui);
        gui.open(viewer);
    }

    /**
     * Closes the player's current inventory, prompts them to type in chat,
     * and fires the callback on the main thread with the typed text.
     */
    public void awaitChatInput(Player player, String prompt, Consumer<String> callback) {
        player.closeInventory();
        openGuis.remove(player.getUniqueId());
        pendingChatInputs.put(player.getUniqueId(), callback);
        player.sendMessage(ColorUtil.parse(prompt));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        Consumer<String> callback = pendingChatInputs.remove(player.getUniqueId());
        if (callback == null) return;
        event.setCancelled(true);
        String text = PlainTextComponentSerializer.plainText().serialize(event.message());
        plugin.getServer().getScheduler().runTask(plugin, () -> callback.accept(text));
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        BaseGui gui = openGuis.get(player.getUniqueId());
        if (gui == null) return;

        // Cancel ALL interactions while a plugin GUI is open (top and bottom inventory).
        event.setCancelled(true);
        // Force-sync the client so it never shows items as moved even briefly.
        player.updateInventory();

        // Only dispatch clicks that land inside the plugin's top inventory.
        if (event.getView().getTopInventory().equals(gui.getInventory())
                && event.getRawSlot() < gui.getInventory().getSize()) {
            gui.handleClick(event.getRawSlot(), player);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (openGuis.containsKey(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        BaseGui gui = openGuis.get(player.getUniqueId());
        // Only deregister when the inventory being closed IS the registered GUI.
        // Using priority MONITOR so other plugins run first; and only removing
        // on an exact match prevents openInventory() from wiping a newly-registered
        // GUI mid-transition (Bukkit fires InventoryCloseEvent for the old inventory
        // synchronously during the openInventory() call, after the new GUI is already
        // in the map).
        if (gui != null && event.getInventory().equals(gui.getInventory())) {
            openGuis.remove(player.getUniqueId());
        }
    }
}
