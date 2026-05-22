package com.dogetennant.dplayerprofiles.config;

import com.dogetennant.dplayerprofiles.model.AchievementConfig;
import com.dogetennant.dplayerprofiles.model.TriggerType;
import com.dogetennant.dplayerprofiles.reward.AchievementRewardStorage;
import com.dogetennant.dplayerprofiles.util.LogUtil;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class AchievementConfigLoader {

    private final Plugin plugin;
    private AchievementRewardStorage rewardStorage;
    private final Map<String, AchievementConfig> achievements = new LinkedHashMap<>();

    public AchievementConfigLoader(Plugin plugin) {
        this.plugin = plugin;
    }

    public void setRewardStorage(AchievementRewardStorage storage) {
        this.rewardStorage = storage;
    }

    public void load() {
        achievements.clear();

        File dir = new File(plugin.getDataFolder(), "achievements");
        if (!dir.exists()) {
            plugin.saveResource("achievements/example.yml", false);
            dir.mkdirs();
        }

        File[] files = dir.listFiles((d, name) -> name.endsWith(".yml"));
        if (files == null) return;

        for (File file : files) {
            if (file.getName().equals("example.yml")) continue;
            try {
                YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
                AchievementConfig ac = parse(cfg, file.getName());
                if (ac != null) achievements.put(ac.id, ac);
            } catch (Exception e) {
                LogUtil.warn("Failed to load achievement file " + file.getName() + ": " + e.getMessage());
            }
        }

        // Rewards are sourced exclusively from rewards.yml (edited via /dp rewards GUI or direct file edit).
        if (rewardStorage != null) {
            for (AchievementConfig ac : achievements.values()) {
                ac.rewards = rewardStorage.getRewards(ac.id);
            }
        }

        LogUtil.info("Loaded " + achievements.size() + " achievement(s).");
    }

    private AchievementConfig parse(YamlConfiguration cfg, String filename) {
        String id = cfg.getString("id");
        if (id == null || id.isBlank()) {
            LogUtil.warn("Achievement in " + filename + " is missing 'id' - skipping.");
            return null;
        }

        AchievementConfig ac = new AchievementConfig();
        ac.id = id;
        ac.displayName = cfg.getString("display-name", id);
        ac.description = cfg.getString("description", "");

        String iconStr = cfg.getString("icon", "PAPER").toUpperCase();
        ac.icon = Material.matchMaterial(iconStr);
        if (ac.icon == null) {
            LogUtil.warn("Unknown material '" + iconStr + "' for achievement " + id + ", using PAPER.");
            ac.icon = Material.PAPER;
        }

        ac.category = cfg.getString("category", null);
        if (ac.category != null && ac.category.isBlank()) ac.category = null;

        ac.group = cfg.getString("group", null);
        if (ac.group != null && ac.group.isBlank()) ac.group = null;

        ac.requires = cfg.getStringList("requires");
        ac.requiredBadges = cfg.getStringList("required-badges").stream()
                .map(String::toLowerCase).toList();
        ac.permission = cfg.getString("permission", "");
        ac.hidden = cfg.getBoolean("hidden", false);
        ac.broadcast = cfg.getBoolean("broadcast", false);
        ac.points = Math.max(0, cfg.getInt("points", 0));

        String hiddenIconStr = cfg.getString("hidden-icon", "GRAY_CONCRETE").toUpperCase();
        ac.hiddenIcon = Material.matchMaterial(hiddenIconStr);
        if (ac.hiddenIcon == null) ac.hiddenIcon = Material.GRAY_CONCRETE;

        ConfigurationSection trigger = cfg.getConfigurationSection("trigger");
        if (trigger != null) {
            String typeStr = trigger.getString("type", "MANUAL").toUpperCase();
            try {
                ac.triggerType = TriggerType.valueOf(typeStr);
            } catch (IllegalArgumentException e) {
                LogUtil.warn("Unknown trigger type '" + typeStr + "' in " + filename + " - defaulting to MANUAL.");
                ac.triggerType = TriggerType.MANUAL;
            }
            ac.triggerTarget = trigger.getString("target", null);
            if (ac.triggerTarget != null) ac.triggerTarget = ac.triggerTarget.toUpperCase();
            ac.triggerCount = trigger.getLong("count", 1);
        } else {
            ac.triggerType = TriggerType.MANUAL;
            ac.triggerTarget = null;
            ac.triggerCount = 1;
        }

        // CHAIN achievements auto-complete when prerequisites are met - no event trigger needed.
        if (ac.triggerType == TriggerType.CHAIN) {
            ac.triggerCount = 1;
        }

        ac.rewards = Collections.emptyList(); // populated from rewards.yml after all files are loaded
        return ac;
    }

    public Map<String, AchievementConfig> getAll() {
        return Collections.unmodifiableMap(achievements);
    }

    public AchievementConfig get(String id) {
        return achievements.get(id);
    }
}
