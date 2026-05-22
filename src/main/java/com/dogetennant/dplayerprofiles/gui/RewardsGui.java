package com.dogetennant.dplayerprofiles.gui;

import com.dogetennant.dplayerprofiles.DPlayerProfiles;
import com.dogetennant.dplayerprofiles.lang.MessageKey;
import com.dogetennant.dplayerprofiles.lang.Placeholder;
import com.dogetennant.dplayerprofiles.model.AchievementConfig;
import com.dogetennant.dplayerprofiles.util.ColorUtil;
import com.dogetennant.dplayerprofiles.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class RewardsGui extends BaseGui {

    private static final int PER_PAGE = 36;
    private static final int PREV_SLOT = 45;
    private static final int INFO_SLOT = 49;
    private static final int NEXT_SLOT = 53;

    private final DPlayerProfiles plugin;
    private int page;

    public RewardsGui(DPlayerProfiles plugin) {
        this.plugin = plugin;
        this.page = 0;
    }

    @Override
    public void open(Player viewer) {
        String title = plugin.getLangManager().getRaw(MessageKey.REWARDS_GUI_TITLE);
        inventory = Bukkit.createInventory(null, 54, ColorUtil.parse(title));
        buildContent();
        viewer.openInventory(inventory);
    }

    private void buildContent() {
        inventory.clear();

        List<AchievementConfig> all = new ArrayList<>(plugin.getAchievementConfigLoader().getAll().values());
        int totalPages = Math.max(1, (int) Math.ceil((double) all.size() / PER_PAGE));
        if (page >= totalPages) page = totalPages - 1;

        int start = page * PER_PAGE;
        int end = Math.min(start + PER_PAGE, all.size());

        for (int i = start; i < end; i++) {
            AchievementConfig ac = all.get(i);
            int slot = 9 + (i - start);
            int rewardCount = ac.rewards.size();

            ItemBuilder builder = new ItemBuilder(ac.icon)
                    .name(ac.displayName)
                    .lore(
                            ac.description,
                            "",
                            rewardCount > 0
                                    ? plugin.getLangManager().getRaw(MessageKey.REWARDS_GUI_REWARD_COUNT,
                                            Placeholder.of("count", String.valueOf(rewardCount)))
                                    : plugin.getLangManager().getRaw(MessageKey.REWARDS_GUI_NO_REWARDS),
                            plugin.getLangManager().getRaw(MessageKey.REWARDS_GUI_CLICK_HINT)
                    );
            inventory.setItem(slot, builder.build());
        }

        if (page > 0) {
            inventory.setItem(PREV_SLOT, new ItemBuilder(Material.ARROW)
                    .name(plugin.getLangManager().getRaw(MessageKey.ACHIEVEMENTS_NAV_PREV))
                    .build());
        }
        if (page < totalPages - 1) {
            inventory.setItem(NEXT_SLOT, new ItemBuilder(Material.ARROW)
                    .name(plugin.getLangManager().getRaw(MessageKey.ACHIEVEMENTS_NAV_NEXT))
                    .build());
        }
        inventory.setItem(INFO_SLOT, new ItemBuilder(Material.PAPER)
                .name(plugin.getLangManager().getRaw(MessageKey.ACHIEVEMENTS_NAV_INFO,
                        Placeholder.of("page", String.valueOf(page + 1)),
                        Placeholder.of("pages", String.valueOf(totalPages))))
                .build());

        fill(54, Material.GRAY_STAINED_GLASS_PANE);
    }

    @Override
    public void handleClick(int slot, Player clicker) {
        if (slot == PREV_SLOT && page > 0) {
            page--;
            buildContent();
            return;
        }
        if (slot == NEXT_SLOT) {
            page++;
            buildContent();
            return;
        }

        // Content slots: 9-44
        if (slot < 9 || slot > 44) return;
        int index = page * PER_PAGE + (slot - 9);
        List<AchievementConfig> all = new ArrayList<>(plugin.getAchievementConfigLoader().getAll().values());
        if (index >= all.size()) return;

        AchievementConfig ac = all.get(index);
        plugin.getGuiManager().open(clicker, new AchievementRewardEditorGui(plugin, ac, this));
    }
}
