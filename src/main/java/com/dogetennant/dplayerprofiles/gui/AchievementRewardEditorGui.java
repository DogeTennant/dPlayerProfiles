package com.dogetennant.dplayerprofiles.gui;

import com.dogetennant.dplayerprofiles.DPlayerProfiles;
import com.dogetennant.dplayerprofiles.lang.MessageKey;
import com.dogetennant.dplayerprofiles.lang.Placeholder;
import com.dogetennant.dplayerprofiles.model.AchievementConfig;
import com.dogetennant.dplayerprofiles.reward.RewardEntry;
import com.dogetennant.dplayerprofiles.reward.RewardType;
import com.dogetennant.dplayerprofiles.util.ColorUtil;
import com.dogetennant.dplayerprofiles.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Per-achievement reward editor.
 * Layout (54 slots):
 *  Slot 4  – achievement icon/info
 *  Slots 9-35 – current rewards (right-click to remove)
 *  Slot 36 – Add Command
 *  Slot 37 – Add Held Item
 *  Slot 38 – Add Badge
 *  Slot 44 – Clear All
 *  Slot 45 – Back
 */
public class AchievementRewardEditorGui extends BaseGui {

    private static final int ICON_SLOT = 4;
    private static final int REWARD_START = 9;
    private static final int REWARD_END = 35;
    private static final int ADD_COMMAND_SLOT = 36;
    private static final int ADD_ITEM_SLOT = 37;
    private static final int ADD_BADGE_SLOT = 38;
    private static final int CLEAR_SLOT = 44;
    private static final int BACK_SLOT = 45;

    private final DPlayerProfiles plugin;
    private final AchievementConfig ac;
    private final RewardsGui parent;
    private final List<RewardEntry> rewards; // mutable working copy

    public AchievementRewardEditorGui(DPlayerProfiles plugin, AchievementConfig ac, RewardsGui parent) {
        this.plugin = plugin;
        this.ac = ac;
        this.parent = parent;
        this.rewards = new ArrayList<>(ac.rewards);
    }

    @Override
    public void open(Player viewer) {
        String title = plugin.getLangManager().getRaw(MessageKey.REWARDS_EDITOR_TITLE,
                Placeholder.of("achievement", ColorUtil.stripFormatting(ac.displayName)));
        inventory = Bukkit.createInventory(null, 54, ColorUtil.parse(title));
        buildContent();
        viewer.openInventory(inventory);
    }

    private void buildContent() {
        inventory.clear();

        // Achievement icon
        inventory.setItem(ICON_SLOT, new ItemBuilder(ac.icon)
                .name(ac.displayName)
                .lore(ac.description)
                .build());

        // Current rewards
        if (rewards.isEmpty()) {
            inventory.setItem(REWARD_START, new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE)
                    .name(plugin.getLangManager().getRaw(MessageKey.REWARDS_EDITOR_EMPTY))
                    .build());
        } else {
            for (int i = 0; i < Math.min(rewards.size(), REWARD_END - REWARD_START + 1); i++) {
                inventory.setItem(REWARD_START + i, buildRewardItem(rewards.get(i)));
            }
        }

        // Action buttons
        inventory.setItem(ADD_COMMAND_SLOT, new ItemBuilder(Material.WRITABLE_BOOK)
                .name(plugin.getLangManager().getRaw(MessageKey.REWARDS_EDITOR_ADD_COMMAND))
                .lore(plugin.getLangManager().getRaw(MessageKey.REWARDS_EDITOR_REMOVE_HINT))
                .build());

        inventory.setItem(ADD_ITEM_SLOT, new ItemBuilder(Material.CHEST)
                .name(plugin.getLangManager().getRaw(MessageKey.REWARDS_EDITOR_ADD_ITEM))
                .build());

        inventory.setItem(ADD_BADGE_SLOT, new ItemBuilder(Material.FLOWER_BANNER_PATTERN)
                .name(plugin.getLangManager().getRaw(MessageKey.REWARDS_EDITOR_ADD_BADGE))
                .build());

        inventory.setItem(CLEAR_SLOT, new ItemBuilder(Material.BARRIER)
                .name(plugin.getLangManager().getRaw(MessageKey.REWARDS_EDITOR_CLEAR))
                .build());

        inventory.setItem(BACK_SLOT, new ItemBuilder(Material.SPECTRAL_ARROW)
                .name(plugin.getLangManager().getRaw(MessageKey.ACHIEVEMENTS_BACK))
                .build());

        fill(54, Material.GRAY_STAINED_GLASS_PANE);
    }

    private ItemStack buildRewardItem(RewardEntry reward) {
        return switch (reward.type) {
            case COMMAND -> new ItemBuilder(Material.WRITABLE_BOOK)
                    .name(plugin.getLangManager().getRaw(MessageKey.REWARDS_EDITOR_COMMAND_LABEL,
                            Placeholder.of("command", reward.command)))
                    .lore(plugin.getLangManager().getRaw(MessageKey.REWARDS_EDITOR_REMOVE_HINT))
                    .build();
            case ITEM -> {
                if (reward.itemStack != null) {
                    ItemStack clone = reward.itemStack.clone();
                    // Append "right-click to remove" hint to the lore
                    var meta = clone.getItemMeta();
                    List<net.kyori.adventure.text.Component> lore = meta.lore() != null
                            ? new ArrayList<>(meta.lore()) : new ArrayList<>();
                    lore.add(net.kyori.adventure.text.Component.empty());
                    lore.add(ColorUtil.parse(plugin.getLangManager().getRaw(MessageKey.REWARDS_EDITOR_REMOVE_HINT)));
                    meta.lore(lore);
                    clone.setItemMeta(meta);
                    yield clone;
                }
                yield new ItemBuilder(reward.material != null ? reward.material : Material.PAPER)
                        .name(plugin.getLangManager().getRaw(MessageKey.REWARDS_EDITOR_ITEM_LABEL))
                        .lore(plugin.getLangManager().getRaw(MessageKey.REWARDS_EDITOR_REMOVE_HINT))
                        .build();
            }
            case BADGE -> new ItemBuilder(Material.FLOWER_BANNER_PATTERN)
                    .name(plugin.getLangManager().getRaw(MessageKey.REWARDS_EDITOR_BADGE_LABEL,
                            Placeholder.of("badge", reward.badgeId)))
                    .lore(plugin.getLangManager().getRaw(MessageKey.REWARDS_EDITOR_REMOVE_HINT))
                    .build();
        };
    }

    @Override
    public void handleClick(int slot, Player clicker) {
        // Right-click on a reward slot removes it
        if (slot >= REWARD_START && slot <= REWARD_END) {
            // We need to know if it was a right-click; GuiManager only passes slot.
            // We handle both left and right as "remove" since the slot clearly shows a reward.
            int index = slot - REWARD_START;
            if (index < rewards.size()) {
                rewards.remove(index);
                saveAndRefresh(clicker);
            }
            return;
        }

        switch (slot) {
            case ADD_COMMAND_SLOT -> {
                String prompt = plugin.getLangManager().getRaw(MessageKey.REWARDS_CMD_INPUT_PROMPT);
                plugin.getGuiManager().awaitChatInput(clicker, prompt, input -> {
                    if (!input.equalsIgnoreCase("cancel")) {
                        // Strip leading slash if admin typed one accidentally
                        String cmd = input.startsWith("/") ? input.substring(1) : input;
                        rewards.add(RewardEntry.command(cmd));
                        saveAndRefresh(clicker);
                    }
                    plugin.getGuiManager().open(clicker, this);
                });
            }
            case ADD_ITEM_SLOT -> {
                ItemStack held = clicker.getInventory().getItemInMainHand();
                if (held.getType() == Material.AIR) {
                    plugin.getLangManager().send(clicker, MessageKey.REWARDS_EDITOR_NO_ITEM);
                } else {
                    rewards.add(RewardEntry.item(held));
                    saveAndRefresh(clicker);
                    buildContent(); // refresh in place
                }
            }
            case ADD_BADGE_SLOT -> {
                String prompt = plugin.getLangManager().getRaw(MessageKey.REWARDS_BADGE_INPUT_PROMPT);
                plugin.getGuiManager().awaitChatInput(clicker, prompt, input -> {
                    if (!input.equalsIgnoreCase("cancel")) {
                        if (plugin.getBadgeConfigLoader().get(input) != null) {
                            rewards.add(RewardEntry.badge(input));
                            saveAndRefresh(clicker);
                        } else {
                            plugin.getLangManager().send(clicker, MessageKey.CMD_BADGE_NOT_FOUND,
                                    Placeholder.of("badge", input));
                        }
                    }
                    plugin.getGuiManager().open(clicker, this);
                });
            }
            case CLEAR_SLOT -> {
                rewards.clear();
                saveAndRefresh(clicker);
            }
            case BACK_SLOT -> plugin.getGuiManager().open(clicker, parent);
        }
    }

    private void saveAndRefresh(Player player) {
        // Update the live achievement config so the change is visible immediately
        ac.rewards = new ArrayList<>(rewards);
        // Persist to rewards.yml
        plugin.getAchievementRewardStorage().setRewards(ac.id, rewards);
        plugin.getLangManager().send(player, MessageKey.REWARDS_EDITOR_SAVED,
                Placeholder.of("achievement", ColorUtil.stripFormatting(ac.displayName)));
        buildContent();
    }
}
