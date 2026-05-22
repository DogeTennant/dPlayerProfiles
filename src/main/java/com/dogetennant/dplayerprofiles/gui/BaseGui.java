package com.dogetennant.dplayerprofiles.gui;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public abstract class BaseGui {

    protected Inventory inventory;

    public abstract void open(Player viewer);

    public abstract void handleClick(int slot, Player clicker);

    public Inventory getInventory() {
        return inventory;
    }

    protected void fill(int size, org.bukkit.Material filler) {
        org.bukkit.inventory.ItemStack glass = new com.dogetennant.dplayerprofiles.util.ItemBuilder(filler)
                .name(" ")
                .build();
        for (int i = 0; i < size; i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, glass);
            }
        }
    }
}
