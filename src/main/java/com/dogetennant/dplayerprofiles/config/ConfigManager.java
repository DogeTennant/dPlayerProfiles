package com.dogetennant.dplayerprofiles.config;

import com.dogetennant.dplayerprofiles.util.LogUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;

public class ConfigManager {

    private final Plugin plugin;
    private MainConfig mainConfig;

    public ConfigManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        FileConfiguration cfg = plugin.getConfig();

        mainConfig = new MainConfig();

        mainConfig.storageType = cfg.getString("storage.type", "sqlite").toLowerCase();
        mainConfig.tablePrefix = cfg.getString("storage.table-prefix", "");

        mainConfig.mysqlHost = cfg.getString("storage.mysql.host", "localhost");
        mainConfig.mysqlPort = cfg.getInt("storage.mysql.port", 3306);
        mainConfig.mysqlDatabase = cfg.getString("storage.mysql.database", "dplayerprofiles");
        mainConfig.mysqlUsername = cfg.getString("storage.mysql.username", "root");
        mainConfig.mysqlPassword = cfg.getString("storage.mysql.password", "");
        mainConfig.mysqlPoolSize = cfg.getInt("storage.mysql.pool-size", 10);
        mainConfig.mysqlConnectionTimeout = cfg.getLong("storage.mysql.connection-timeout", 30000);
        mainConfig.mysqlMaxLifetime = cfg.getLong("storage.mysql.max-lifetime", 1800000);

        mainConfig.language = cfg.getString("language", "en_us");
        mainConfig.streaksEnabled = cfg.getBoolean("streaks-enabled", true);
        mainConfig.leaderboardSize = cfg.getInt("leaderboard-size", 10);
        mainConfig.playtimeUpdateInterval = cfg.getInt("playtime-update-interval", 60);

        mainConfig.antiAfkEnabled = cfg.getBoolean("anti-afk.enabled", true);
        mainConfig.antiAfkTimeout = cfg.getInt("anti-afk.timeout", 300);

        mainConfig.coreprotectEnabled = cfg.getBoolean("coreprotect.enabled", true);
        mainConfig.coreprotectLookupTime = cfg.getInt("coreprotect.lookup-time", 0);

        mainConfig.achievementNotification = cfg.getString("achievement-notification", "both").toLowerCase();

        mainConfig.pointsEnabled = cfg.getBoolean("points.enabled", true);
        mainConfig.pointsPerNode = cfg.getInt("points.points-per-node", 100);
        mainConfig.pointsNodeCount = cfg.getInt("points.node-count", 50);
        mainConfig.pointsMilestoneInterval = cfg.getInt("points.milestone-interval", 10);

        ConfigurationSection catPerms = cfg.getConfigurationSection("category-permissions");
        if (catPerms != null) {
            Map<String, String> map = new HashMap<>();
            for (String cat : catPerms.getKeys(false)) {
                String perm = catPerms.getString(cat);
                if (perm != null && !perm.isBlank()) map.put(cat, perm);
            }
            mainConfig.categoryPermissions = map;
        }

        LogUtil.info("Config loaded. Storage: " + mainConfig.storageType);
    }

    public MainConfig get() {
        return mainConfig;
    }
}
