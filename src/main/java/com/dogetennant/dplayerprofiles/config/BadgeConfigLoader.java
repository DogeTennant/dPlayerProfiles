package com.dogetennant.dplayerprofiles.config;

import com.dogetennant.dplayerprofiles.model.BadgeConfig;
import com.dogetennant.dplayerprofiles.util.LogUtil;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class BadgeConfigLoader {

    private final Plugin plugin;
    private final Map<String, BadgeConfig> badges = new LinkedHashMap<>();

    public BadgeConfigLoader(Plugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        badges.clear();

        File dir = new File(plugin.getDataFolder(), "badges");
        if (!dir.exists()) {
            plugin.saveResource("badges/example.yml", false);
            dir.mkdirs();
        }

        File[] files = dir.listFiles((d, name) -> name.endsWith(".yml"));
        if (files == null) return;

        for (File file : files) {
            if (file.getName().equals("example.yml")) continue;
            try {
                YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
                BadgeConfig bc = parse(cfg, file.getName());
                if (bc != null) badges.put(bc.id, bc);
            } catch (Exception e) {
                LogUtil.warn("Failed to load badge file " + file.getName() + ": " + e.getMessage());
            }
        }

        LogUtil.info("Loaded " + badges.size() + " badge(s).");
    }

    private BadgeConfig parse(YamlConfiguration cfg, String filename) {
        String id = cfg.getString("id");
        if (id == null || id.isBlank()) {
            LogUtil.warn("Badge in " + filename + " is missing 'id' - skipping.");
            return null;
        }

        BadgeConfig bc = new BadgeConfig();
        bc.id = id.toLowerCase();
        bc.displayName = cfg.getString("display-name", id);
        bc.description = cfg.getString("description", "");

        String iconStr = cfg.getString("icon", "PAPER").toUpperCase();
        bc.icon = Material.matchMaterial(iconStr);
        if (bc.icon == null) {
            LogUtil.warn("Unknown material '" + iconStr + "' for badge " + id + ", using PAPER.");
            bc.icon = Material.PAPER;
        }

        return bc;
    }

    public Map<String, BadgeConfig> getAll() {
        return Collections.unmodifiableMap(badges);
    }

    public BadgeConfig get(String id) {
        return badges.get(id.toLowerCase());
    }
}
