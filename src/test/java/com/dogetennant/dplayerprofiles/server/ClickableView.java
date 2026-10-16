package com.dogetennant.dplayerprofiles.server;

import org.bukkit.entity.HumanEntity;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.mockbukkit.mockbukkit.inventory.SimpleInventoryViewMock;

/**
 * A chest-like view of a plugin screen above the player's inventory, numbering the slots as the
 * server does (MockBukkit 4.116 leaves {@code convertSlot} out, which every click event needs).
 * The cursor is the player's.
 */
final class ClickableView extends SimpleInventoryViewMock {

    ClickableView(HumanEntity player, Inventory top) {
        super(player, top, player.getInventory(), InventoryType.CHEST);
    }

    private int topSize() {
        return getTopInventory().getSize();
    }

    /** Raw slot to the slot of its own inventory: below the screen come 27 storage slots, then the hotbar. */
    @Override
    public int convertSlot(int rawSlot) {
        if (rawSlot < topSize()) return rawSlot;
        int slot = rawSlot - topSize();
        return slot < 27 ? slot + 9 : slot - 27;
    }

    @Override
    public Inventory getInventory(int rawSlot) {
        if (rawSlot < 0) return null;
        return rawSlot < topSize() ? getTopInventory() : getBottomInventory();
    }

    @Override
    public ItemStack getItem(int rawSlot) {
        return getInventory(rawSlot).getItem(convertSlot(rawSlot));
    }

    @Override
    public void setItem(int rawSlot, ItemStack item) {
        getInventory(rawSlot).setItem(convertSlot(rawSlot), item);
    }

    @Override
    public InventoryType.SlotType getSlotType(int rawSlot) {
        return InventoryType.SlotType.CONTAINER;
    }

    @Override
    public ItemStack getCursor() {
        return getPlayer().getItemOnCursor();
    }

    @Override
    public void setCursor(ItemStack item) {
        getPlayer().setItemOnCursor(item);
    }
}
