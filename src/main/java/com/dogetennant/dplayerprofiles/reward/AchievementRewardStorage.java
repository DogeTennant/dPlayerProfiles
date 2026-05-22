package com.dogetennant.dplayerprofiles.reward;

import com.dogetennant.dplayerprofiles.util.LogUtil;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Persists reward overrides set via the in-game /dp rewards GUI to rewards.yml.
 * These override whatever rewards are defined in the achievement config files.
 */
public class AchievementRewardStorage {

    private final Plugin plugin;
    private final File file;
    private YamlConfiguration config;

    public AchievementRewardStorage(Plugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "rewards.yml");
    }

    public void load() {
        config = file.exists() ? YamlConfiguration.loadConfiguration(file) : new YamlConfiguration();
    }

    public boolean hasOverride(String achievementId) {
        return config.isConfigurationSection(achievementId);
    }

    public List<RewardEntry> getRewards(String achievementId) {
        if (!config.isConfigurationSection(achievementId)) return Collections.emptyList();
        List<RewardEntry> rewards = new ArrayList<>();

        List<String> commands = config.getStringList(achievementId + ".commands");
        for (String cmd : commands) {
            rewards.add(RewardEntry.command(cmd));
        }

        List<?> items = config.getList(achievementId + ".items");
        if (items != null) {
            for (Object obj : items) {
                if (obj instanceof ItemStack stack) {
                    rewards.add(RewardEntry.item(stack));
                }
            }
        }

        List<String> badges = config.getStringList(achievementId + ".badges");
        for (String badgeId : badges) {
            rewards.add(RewardEntry.badge(badgeId.toLowerCase()));
        }

        return rewards;
    }

    /** Returns the free-form display lore lines for a reward entry, or empty if none set. */
    public List<String> getRewardLore(String achievementId) {
        return config.getStringList(achievementId + ".lore");
    }

    public void setRewards(String achievementId, List<RewardEntry> rewards) {
        config.set(achievementId, null); // clear existing

        List<String> commands = new ArrayList<>();
        List<ItemStack> items = new ArrayList<>();
        List<String> badges = new ArrayList<>();

        for (RewardEntry r : rewards) {
            switch (r.type) {
                case COMMAND -> commands.add(r.command);
                case ITEM -> {
                    if (r.itemStack != null) items.add(r.itemStack);
                }
                case BADGE -> badges.add(r.badgeId);
            }
        }

        if (!commands.isEmpty()) config.set(achievementId + ".commands", commands);
        if (!items.isEmpty()) config.set(achievementId + ".items", items);
        if (!badges.isEmpty()) config.set(achievementId + ".badges", badges);

        save();
    }

    private void save() {
        try {
            config.save(file);
        } catch (IOException e) {
            LogUtil.severe("Failed to save rewards.yml", e);
        }
    }
}
