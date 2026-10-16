package com.dogetennant.dplayerprofiles.config;

import com.dogetennant.dplayerprofiles.util.LogUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ConfigManager {

    private final Plugin plugin;
    private MainConfig mainConfig;

    public ConfigManager(Plugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Writes a single value to config.yml as it is on disk now, so hand edits and comments made
     * since the last load stay. A file with a YAML error is left alone: the value applies until
     * the next start or reload.
     *
     * @return false when config.yml could not be read (nothing was written)
     */
    public boolean set(String path, Object value) {
        plugin.getConfig().set(path, value);

        File file = new File(plugin.getDataFolder(), "config.yml");
        YamlConfiguration onDisk = new YamlConfiguration();
        try {
            if (file.exists()) onDisk.load(file);
        } catch (IOException | InvalidConfigurationException e) {
            LogUtil.warn("config.yml could not be read, so '" + path + "' was not saved to it"
                    + " (it applies until the next start): " + e.getMessage());
            return false;
        }
        onDisk.set(path, value);
        try {
            onDisk.save(file);
        } catch (IOException e) {
            LogUtil.warn("Could not save '" + path + "' to config.yml: " + e.getMessage());
        }
        return true;
    }

    /**
     * Reads config.yml. A file with a YAML error is not taken for "no settings": the built-in
     * ones would point the plugin at another database (storage.type falls back to sqlite), so the
     * settings from before stay and the caller decides (at start the plugin stays off).
     *
     * @return false when config.yml has an error (nothing changed)
     */
    public boolean load() {
        plugin.saveDefaultConfig();
        try {
            new YamlConfiguration().load(new File(plugin.getDataFolder(), "config.yml"));
        } catch (IOException | InvalidConfigurationException e) {
            LogUtil.severe("config.yml cannot be read, so its settings are not used: " + e.getMessage());
            return false;
        }
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
        mainConfig.playtimeIgnoreVanished = cfg.getBoolean("playtime-ignore-vanished", true);
        mainConfig.lastSeenIgnoreVanished = cfg.getBoolean("last-seen-ignore-vanished", true);
        mainConfig.loginIgnoreVanished = cfg.getBoolean("login-ignore-vanished", true);

        mainConfig.antiAfkEnabled = cfg.getBoolean("anti-afk.enabled", true);
        mainConfig.antiAfkTimeout = cfg.getInt("anti-afk.timeout", 300);

        mainConfig.antiFarmEnabled = cfg.getBoolean("anti-farm.enabled", true);
        mainConfig.antiFarmMaxPerChunk = cfg.getInt("anti-farm.max-tracked-per-chunk", 8192);

        if (cfg.isConfigurationSection("coreprotect")) {
            LogUtil.warn("The 'coreprotect' config section is obsolete - anti-farm tracking is now built in "
                    + "and no longer queries CoreProtect. Delete that section and use 'anti-farm' instead.");
        }

        mainConfig.achievementNotification = cfg.getString("achievement-notification", "both").toLowerCase();

        mainConfig.pointsEnabled = cfg.getBoolean("points.enabled", true);
        mainConfig.pointsPerNode = cfg.getInt("points.points-per-node", 100);
        mainConfig.pointsNodeCount = cfg.getInt("points.node-count", 50);
        mainConfig.pointsMilestoneInterval = cfg.getInt("points.milestone-interval", 10);

        mainConfig.webStatsEnabled = cfg.getBoolean("web-stats.enabled", false);
        mainConfig.webStatsInterval = Math.max(10, cfg.getInt("web-stats.interval", 300));
        mainConfig.webStatsBackfillOnStartup = cfg.getBoolean("web-stats.backfill-on-startup", true);
        mainConfig.webStatsBackfillBalances = cfg.getBoolean("web-stats.backfill-balances", true);
        mainConfig.webStatsBackfillBalancesPerTick = Math.max(1, cfg.getInt("web-stats.backfill-balances-per-tick", 10));
        mainConfig.webStatsExportCatalog = cfg.getBoolean("web-stats.export-catalog", true);

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
        return true;
    }

    public MainConfig get() {
        return mainConfig;
    }
}
