package com.dogetennant.dplayerprofiles.stats;

import com.dogetennant.dplayerprofiles.DPlayerProfiles;
import com.dogetennant.dplayerprofiles.config.MainConfig;
import com.dogetennant.dplayerprofiles.database.DatabaseManager;
import com.dogetennant.dplayerprofiles.integration.VaultHook;
import com.dogetennant.dplayerprofiles.model.AchievementConfig;
import com.dogetennant.dplayerprofiles.model.BadgeConfig;
import com.dogetennant.dplayerprofiles.model.StatsSnapshot;
import com.dogetennant.dplayerprofiles.util.LogUtil;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Keeps the '<prefix>player_stats' table (and the achievement/badge catalogs) up to date for
 * external consumers such as a website. See the 'web-stats' section of config.yml.
 *
 * Online players are snapshotted every 'interval' seconds and on quit, a few players per tick
 * so a full server never costs more than a fraction of one tick. Offline players are imported
 * from the world's stats files by {@link #backfill}.
 */
public class WebStatsExporter implements Listener {

    /** Online players snapshotted per tick during a periodic run. */
    private static final int SNAPSHOTS_PER_TICK = 5;

    private final DPlayerProfiles plugin;
    private final DatabaseManager db;

    private File statsFolder;
    private File userCacheFile;
    private BukkitTask intervalTask;
    private BukkitTask rollingSnapshotTask;
    private BukkitTask balanceTask;
    private final AtomicBoolean backfillRunning = new AtomicBoolean(false);

    public record BackfillResult(int playersImported, int balancesUpdated) {}

    public WebStatsExporter(DPlayerProfiles plugin) {
        this.plugin = plugin;
        this.db = plugin.getDatabaseManager();
    }

    private MainConfig cfg() {
        return plugin.getConfigManager().get();
    }

    public boolean isEnabled() {
        return cfg().webStatsEnabled;
    }

    public boolean isBackfillRunning() {
        return backfillRunning.get();
    }

    /** Call once from onEnable, after worlds are loaded and integrations are initialised. */
    public void start() {
        List<World> worlds = Bukkit.getWorlds();
        statsFolder = worlds.isEmpty() ? null
                : findStatsFolder(worlds.get(0).getWorldFolder(), Bukkit.getWorldContainer());
        userCacheFile = new File(Bukkit.getWorldContainer(), "usercache.json");
        Bukkit.getPluginManager().registerEvents(this, plugin);
        applyConfig();

        if (isEnabled() && cfg().webStatsBackfillOnStartup) {
            backfill(false, r -> LogUtil.info("Web stats backfill finished: " + r.playersImported()
                    + " players imported from stats files, " + r.balancesUpdated() + " balances updated."));
        }
    }

    /**
     * Locates the directory holding the per-player statistics files. Two things differ between
     * Minecraft versions: what the API reports as the world folder (the level root up to 1.21,
     * the dimension directory such as world/dimensions/minecraft/overworld from 26.1 on), and
     * where the files live relative to the level root ('stats' up to 1.21, 'players/stats'
     * from 26.1 on). So walk up from the reported folder to the level root (where level.dat
     * is), never leaving the server directory, and pick whichever layout exists there.
     */
    static File findStatsFolder(File worldFolder, File serverRoot) {
        File root = serverRoot.getAbsoluteFile();
        for (File dir = worldFolder.getAbsoluteFile(); dir != null; dir = dir.getParentFile()) {
            File modern = new File(dir, "players" + File.separator + "stats");
            File legacy = new File(dir, "stats");
            if (modern.isDirectory()) return modern;
            if (legacy.isDirectory()) return legacy;
            if (new File(dir, "level.dat").isFile()) {
                // Level root found but nobody has played yet - guess by the layout in use
                return new File(dir, "players").isDirectory() ? modern : legacy;
            }
            if (dir.equals(root)) break;
        }
        return new File(worldFolder, "stats");
    }

    /** (Re)starts the periodic snapshot and re-exports the catalog. Called on start and /dp reload. */
    public void applyConfig() {
        cancelTasks();
        if (!isEnabled()) return;

        long ticks = cfg().webStatsInterval * 20L;
        intervalTask = Bukkit.getScheduler().runTaskTimer(plugin, this::snapshotOnlinePlayers, ticks, ticks);
        if (cfg().webStatsExportCatalog) exportCatalog();
        LogUtil.info("Web stats export enabled (snapshot every " + cfg().webStatsInterval + "s).");
    }

    public void shutdown() {
        cancelTasks();
        if (!isEnabled()) return;

        // Our scheduler is already closed while disabling, so write the last snapshot synchronously
        long now = System.currentTimeMillis();
        try {
            for (Player player : Bukkit.getOnlinePlayers()) {
                db.upsertPlayerStats(snapshot(player), now);
            }
        } catch (SQLException e) {
            LogUtil.severe("Failed to write web stats on shutdown", e);
        }
    }

    private void cancelTasks() {
        if (intervalTask != null) { intervalTask.cancel(); intervalTask = null; }
        if (rollingSnapshotTask != null) { rollingSnapshotTask.cancel(); rollingSnapshotTask = null; }
        if (balanceTask != null) { balanceTask.cancel(); balanceTask = null; }
    }

    //  Online players

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        if (!isEnabled()) return;
        StatsSnapshot s = snapshot(event.getPlayer());
        long now = System.currentTimeMillis();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                db.upsertPlayerStats(s, now);
            } catch (SQLException e) {
                LogUtil.severe("Failed to save web stats for " + s.username(), e);
            }
        });
    }

    private void snapshotOnlinePlayers() {
        if (rollingSnapshotTask != null) return; // previous run still in progress - skip this interval
        Iterator<? extends Player> players = new ArrayList<>(Bukkit.getOnlinePlayers()).iterator();
        if (!players.hasNext()) return;

        rollingSnapshotTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            List<StatsSnapshot> batch = new ArrayList<>(SNAPSHOTS_PER_TICK);
            for (int i = 0; i < SNAPSHOTS_PER_TICK && players.hasNext(); i++) {
                Player player = players.next();
                if (player.isOnline()) batch.add(snapshot(player)); // quitters were saved by onQuit
            }
            if (!batch.isEmpty()) saveAsync(batch);
            if (!players.hasNext()) {
                rollingSnapshotTask.cancel();
                rollingSnapshotTask = null;
            }
        }, 0L, 1L);
    }

    /** Main thread only. */
    private StatsSnapshot snapshot(Player player) {
        StatsSnapshot s = VanillaStatsReader.fromOnline(player);
        VaultHook vault = plugin.getIntegrationManager().getVaultHook();
        return vault == null ? s : s.withBalance(vault.getBalance(player));
    }

    private void saveAsync(List<StatsSnapshot> snapshots) {
        long now = System.currentTimeMillis();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                for (StatsSnapshot s : snapshots) db.upsertPlayerStats(s, now);
            } catch (SQLException e) {
                LogUtil.severe("Failed to save web stats", e);
            }
        });
    }

    //  Catalog

    public void exportCatalog() {
        List<AchievementConfig> achievements = new ArrayList<>(plugin.getAchievementConfigLoader().getAll().values());
        List<BadgeConfig> badges = new ArrayList<>(plugin.getBadgeConfigLoader().getAll().values());
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                db.replaceAchievementCatalog(achievements);
                db.replaceBadgeCatalog(badges);
                LogUtil.debug("Exported " + achievements.size() + " achievements and " + badges.size()
                        + " badges to the web catalog.");
            } catch (SQLException e) {
                LogUtil.severe("Failed to export the achievement/badge catalog", e);
            }
        });
    }

    //  Offline players

    /**
     * Imports every offline player from the world's stats files (only files newer than the
     * stored row), then looks up balances via Vault on the main thread, a few per tick.
     *
     * @param refreshAllBalances true = re-read every balance; false = only balances never recorded,
     *                           and only if 'backfill-balances' is enabled
     * @param callback           run on the main thread when everything is done; may be null
     * @return false if a backfill is already running
     */
    public boolean backfill(boolean refreshAllBalances, Consumer<BackfillResult> callback) {
        if (!backfillRunning.compareAndSet(false, true)) return false;

        Set<UUID> online = Bukkit.getOnlinePlayers().stream()
                .map(Player::getUniqueId).collect(Collectors.toSet());
        MainConfig cfg = cfg();
        boolean lookUpBalances = (refreshAllBalances || cfg.webStatsBackfillBalances)
                && plugin.getIntegrationManager().getVaultHook() != null;
        int perTick = cfg.webStatsBackfillBalancesPerTick;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            int imported = 0;
            List<UUID> needBalance = List.of();
            try {
                imported = importStatsFiles(online);
                if (lookUpBalances) needBalance = db.getStatsUuids(!refreshAllBalances);
            } catch (SQLException e) {
                LogUtil.severe("Web stats backfill failed", e);
            }

            int importedFinal = imported;
            List<UUID> queue = needBalance;
            if (queue.isEmpty()) {
                finishBackfill(new BackfillResult(importedFinal, 0), callback);
            } else {
                Bukkit.getScheduler().runTask(plugin, () -> runBalancePass(queue, perTick, importedFinal, callback));
            }
        });
        return true;
    }

    /** Async. Returns the number of players written. */
    private int importStatsFiles(Set<UUID> online) throws SQLException {
        if (statsFolder == null || !statsFolder.isDirectory()) {
            LogUtil.warn("Web stats backfill: stats folder not found at " + statsFolder);
            return 0;
        }
        File[] files = statsFolder.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null) return 0;

        Map<UUID, Long> existing = db.getStatsUpdatedAt();
        Map<UUID, String> names = db.getKnownUsernames();
        readUserCache(names);

        int imported = 0;
        for (File file : files) {
            UUID uuid = VanillaStatsReader.uuidFromFileName(file.getName());
            if (uuid == null || online.contains(uuid)) continue; // online players are snapshotted live

            long modified = file.lastModified();
            Long stored = existing.get(uuid);
            if (stored != null && stored >= modified) continue;   // nothing new since last import

            try {
                db.upsertPlayerStats(VanillaStatsReader.fromStatsFile(file, uuid, names.get(uuid)), modified);
                imported++;
            } catch (IOException e) {
                LogUtil.warn("Web stats backfill: skipping " + file.getName() + " - " + e.getMessage());
            }
        }
        return imported;
    }

    /** Adds names from the server's usercache.json for players the plugin has no profile for. */
    private void readUserCache(Map<UUID, String> names) {
        if (!userCacheFile.isFile()) return;
        try (Reader reader = Files.newBufferedReader(userCacheFile.toPath(), StandardCharsets.UTF_8)) {
            JsonElement root = JsonParser.parseReader(reader);
            if (!root.isJsonArray()) return;
            for (JsonElement el : root.getAsJsonArray()) {
                if (!el.isJsonObject()) continue;
                JsonObject entry = el.getAsJsonObject();
                if (!entry.has("uuid") || !entry.has("name")) continue;
                try {
                    names.putIfAbsent(UUID.fromString(entry.get("uuid").getAsString()),
                            entry.get("name").getAsString());
                } catch (IllegalArgumentException ignored) { /* not a uuid */ }
            }
        } catch (IOException | RuntimeException e) {
            LogUtil.warn("Web stats backfill: could not read usercache.json - " + e.getMessage());
        }
    }

    /** Main thread. Looks up 'perTick' balances per tick so the server never stalls. */
    private void runBalancePass(List<UUID> queue, int perTick, int imported, Consumer<BackfillResult> callback) {
        VaultHook vault = plugin.getIntegrationManager().getVaultHook();
        if (vault == null || !vault.isAvailable()) {
            finishBackfill(new BackfillResult(imported, 0), callback);
            return;
        }

        Iterator<UUID> it = queue.iterator();
        int[] updated = {0};
        balanceTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            List<Map.Entry<UUID, Double>> batch = new ArrayList<>(perTick);
            for (int i = 0; i < perTick && it.hasNext(); i++) {
                UUID uuid = it.next();
                Double balance = vault.getBalance(Bukkit.getOfflinePlayer(uuid));
                if (balance != null) batch.add(Map.entry(uuid, balance));
            }
            if (!batch.isEmpty()) {
                updated[0] += batch.size();
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    try {
                        for (Map.Entry<UUID, Double> e : batch) db.updatePlayerBalance(e.getKey(), e.getValue());
                    } catch (SQLException ex) {
                        LogUtil.severe("Failed to save balances during web stats backfill", ex);
                    }
                });
            }
            if (!it.hasNext()) {
                balanceTask.cancel();
                balanceTask = null;
                finishBackfill(new BackfillResult(imported, updated[0]), callback);
            }
        }, 1L, 1L);
    }

    private void finishBackfill(BackfillResult result, Consumer<BackfillResult> callback) {
        backfillRunning.set(false);
        if (callback != null) Bukkit.getScheduler().runTask(plugin, () -> callback.accept(result));
    }
}
