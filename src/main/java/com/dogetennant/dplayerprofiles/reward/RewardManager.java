package com.dogetennant.dplayerprofiles.reward;

import com.dogetennant.dplayerprofiles.lang.LanguageManager;
import com.dogetennant.dplayerprofiles.lang.MessageKey;
import com.dogetennant.dplayerprofiles.lang.Placeholder;
import com.dogetennant.dplayerprofiles.util.ColorUtil;
import com.dogetennant.dplayerprofiles.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.function.BiConsumer;

public class RewardManager {

    private final LanguageManager lang;
    // Injected so we don't create a circular dependency at construction time.
    // Set by DPlayerProfiles after both managers are created.
    private BiConsumer<Player, String> badgeGranter;

    public RewardManager(LanguageManager lang) {
        this.lang = lang;
    }

    public void setBadgeGranter(BiConsumer<Player, String> badgeGranter) {
        this.badgeGranter = badgeGranter;
    }

    public void grant(Player player, List<RewardEntry> rewards) {
        for (RewardEntry reward : rewards) {
            switch (reward.type) {
                case COMMAND -> {
                    String cmd = reward.command.replace("{player}", player.getName());
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
                    lang.send(player, MessageKey.ACHIEVEMENT_REWARD_COMMAND);
                }
                case ITEM -> {
                    ItemStack item;
                    if (reward.itemStack != null) {
                        item = reward.itemStack.clone();
                    } else {
                        ItemBuilder builder = new ItemBuilder(reward.material).amount(reward.amount);
                        if (reward.itemName != null) builder.name(reward.itemName);
                        if (reward.itemLore != null) builder.lore(reward.itemLore.toArray(new String[0]));
                        item = builder.build();
                    }
                    player.getInventory().addItem(item);
                    String displayName = reward.itemStack != null && reward.itemStack.hasItemMeta()
                            && reward.itemStack.getItemMeta().hasDisplayName()
                            ? ColorUtil.stripFormatting(reward.itemStack.getItemMeta().getDisplayName())
                            : (reward.itemName != null
                                    ? ColorUtil.stripFormatting(reward.itemName)
                                    : reward.material.name());
                    lang.send(player, MessageKey.ACHIEVEMENT_REWARD_ITEM,
                            Placeholder.of("item", displayName));
                }
                case BADGE -> {
                    if (badgeGranter != null && reward.badgeId != null) {
                        badgeGranter.accept(player, reward.badgeId);
                    }
                }
            }
        }
    }
}
