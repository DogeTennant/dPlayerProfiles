package com.dogetennant.dplayerprofiles.reward;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class RewardEntry {

    public final RewardType type;

    // COMMAND
    public String command;

    // ITEM - either a raw stack (GUI-configured) or material+meta (config-defined)
    public ItemStack itemStack;   // non-null → use directly
    public Material material;
    public int amount;
    public String itemName;
    public List<String> itemLore;

    // BADGE
    public String badgeId;

    private RewardEntry(RewardType type) {
        this.type = type;
    }

    public static RewardEntry command(String command) {
        RewardEntry e = new RewardEntry(RewardType.COMMAND);
        e.command = command;
        return e;
    }

public static RewardEntry item(Material material, int amount, String name, List<String> lore) {
        RewardEntry e = new RewardEntry(RewardType.ITEM);
        e.material = material;
        e.amount = amount;
        e.itemName = name;
        e.itemLore = lore;
        return e;
    }

    public static RewardEntry item(ItemStack stack) {
        RewardEntry e = new RewardEntry(RewardType.ITEM);
        e.itemStack = stack.clone();
        e.material = stack.getType();
        e.amount = stack.getAmount();
        return e;
    }

    public static RewardEntry badge(String badgeId) {
        RewardEntry e = new RewardEntry(RewardType.BADGE);
        e.badgeId = badgeId;
        return e;
    }
}
