package com.dogetennant.dplayerprofiles.command.subcommand;

import com.dogetennant.dplayerprofiles.DPlayerProfiles;
import com.dogetennant.dplayerprofiles.command.CommandUtil;
import com.dogetennant.dplayerprofiles.command.SubCommand;
import com.dogetennant.dplayerprofiles.config.MainConfig;
import com.dogetennant.dplayerprofiles.database.DatabaseManager;
import com.dogetennant.dplayerprofiles.database.MySQLManager;
import com.dogetennant.dplayerprofiles.database.SQLiteManager;
import com.dogetennant.dplayerprofiles.lang.MessageKey;
import com.dogetennant.dplayerprofiles.lang.Placeholder;
import com.dogetennant.dplayerprofiles.util.LogUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import com.dogetennant.dplayerprofiles.model.AchievementConfig;
import com.dogetennant.dplayerprofiles.model.TriggerType;

import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * /dp migrate [source-prefix|none] [add] - copies all profiles, achievement progress and badges
 * from the storage backend that is NOT currently active into the active one. Switch
 * 'storage.type' in config.yml, restart, then run this once. Merging never lowers existing
 * data, so it is safe to re-run.
 *
 * The source tables may carry a different prefix than the one configured now (typically the
 * old SQLite data has no prefix at all, while each server gets its own prefix on MySQL), so
 * every table set found in the source is merged unless one prefix is given explicitly.
 *
 * 'add' is for a source that started EMPTY while the target already held the history - e.g.
 * the plugin ran on a regenerated (SQLite) config for a while. Its counters are then deltas,
 * so playtime and cumulative achievement progress are added onto the target instead of taking
 * the higher value, which would simply discard them. Run 'add' exactly once per source and
 * remove the source afterwards, or the same time is added again.
 */
public class MigrateSubCommand implements SubCommand {

    @Override
    public String getName() { return "migrate"; }

    @Override
    public String getPermission() { return "dplayerprofiles.admin"; }

    @Override
    public MessageKey getDescriptionKey() { return MessageKey.CMD_DESC_MIGRATE; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        DPlayerProfiles plugin = DPlayerProfiles.getInstance();
        MainConfig cfg = plugin.getConfigManager().get();

        // Online players hold cached profiles that would overwrite merged rows on their next save
        if (!Bukkit.getOnlinePlayers().isEmpty()) {
            CommandUtil.reply(sender, MessageKey.CMD_MIGRATE_PLAYERS_ONLINE);
            return;
        }

        boolean targetIsMySql = cfg.storageType.equals("mysql");
        String sourceName = targetIsMySql ? "SQLite" : "MySQL";
        String targetName = targetIsMySql ? "MySQL" : "SQLite";

        File sqliteFile = new File(plugin.getDataFolder(), "data.db");
        if (targetIsMySql && !sqliteFile.isFile()) {
            CommandUtil.reply(sender, MessageKey.CMD_MIGRATE_NO_SOURCE,
                    Placeholder.of("file", sqliteFile.getPath()));
            return;
        }

        boolean additive = false;
        String prefixArg = null;
        for (int i = 1; i < args.length; i++) {
            if (args[i].equalsIgnoreCase("add")) additive = true;
            else prefixArg = args[i].equalsIgnoreCase("none") ? "" : args[i];
        }
        boolean add = additive;
        String requestedPrefix = prefixArg;

        CommandUtil.reply(sender, MessageKey.CMD_MIGRATE_STARTED,
                Placeholder.of("source", sourceName), Placeholder.of("target", targetName));

        DatabaseManager target = plugin.getDatabaseManager();
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            DatabaseManager source = targetIsMySql
                    ? new SQLiteManager(plugin.getDataFolder(), cfg.tablePrefix)
                    : new MySQLManager(cfg);
            try {
                // The configured prefix must point at this plugin's own tables in the target,
                // otherwise the merge would write into (or read) some other plugin's 'players'.
                List<String> targetSets = target.detectTablePrefixes();
                if (!targetSets.contains(target.getTablePrefix())) {
                    CommandUtil.reply(sender, MessageKey.CMD_MIGRATE_TARGET_MISMATCH,
                            Placeholder.of("target", targetName),
                            Placeholder.of("table_prefix", shown(target.getTablePrefix())),
                            Placeholder.of("prefixes", shownList(targetSets)));
                    return;
                }

                source.connect(); // read only - no tables are created in the source

                // Every table set in the source is merged; merging never lowers data, so the
                // result is simply the union even when a player appears in several sets.
                List<String> prefixes = requestedPrefix != null
                        ? List.of(requestedPrefix)
                        : source.detectTablePrefixes();
                if (prefixes.isEmpty()) {
                    CommandUtil.reply(sender, MessageKey.CMD_MIGRATE_NO_TABLES, Placeholder.of("source", sourceName));
                    return;
                }
                String shownPrefixes = shownList(prefixes);
                if (prefixes.size() > 1) {
                    if (add) {
                        // Only the set that started empty holds deltas; adding an old full copy
                        // on top would inflate everyone's counters, so make the admin pick.
                        CommandUtil.reply(sender, MessageKey.CMD_MIGRATE_ADD_NEEDS_PREFIX,
                                Placeholder.of("source", sourceName), Placeholder.of("prefixes", shownPrefixes));
                        return;
                    }
                    CommandUtil.reply(sender, MessageKey.CMD_MIGRATE_SEVERAL_SETS,
                            Placeholder.of("source", sourceName), Placeholder.of("prefixes", shownPrefixes));
                }

                int players = 0, progress = 0, badges = 0;
                for (String prefix : prefixes) {
                    source.setTablePrefix(prefix);
                    var p = source.dumpPlayers();
                    var a = source.dumpAchievementProgress();
                    if (add) {
                        p = addPlaytime(p, target.dumpPlayers());
                        a = addProgress(a, target.dumpAchievementProgress(), plugin);
                        LogUtil.info("Additive merge: source counters are added onto the " + targetName + " values.");
                    }
                    target.mergePlayers(p);
                    target.mergeAchievementProgress(a);
                    var b = source.dumpBadges();
                    target.mergeBadges(b);
                    players += p.size();
                    progress += a.size();
                    badges += b.size();
                    LogUtil.info("Merged " + p.size() + " players, " + a.size() + " achievement rows and "
                            + b.size() + " badges from " + sourceName + " tables with prefix "
                            + (prefix.isEmpty() ? "(none)" : "'" + prefix + "'"));
                }

                LogUtil.info("Migration from " + sourceName + " to " + targetName + " (prefix '"
                        + target.getTablePrefix() + "') finished: " + players + " players, " + progress
                        + " achievement rows, " + badges + " badges.");
                CommandUtil.reply(sender, MessageKey.CMD_MIGRATE_SUCCESS,
                        Placeholder.of("players", String.valueOf(players)),
                        Placeholder.of("achievements", String.valueOf(progress)),
                        Placeholder.of("badges", String.valueOf(badges)),
                        Placeholder.of("tables", shownPrefixes));
            } catch (SQLException e) {
                LogUtil.severe("Migration from " + sourceName + " to " + targetName + " failed", e);
                CommandUtil.reply(sender, MessageKey.CMD_MIGRATE_FAILED);
            } finally {
                source.shutdown();
            }
        });
    }

    private static String shown(String prefix) {
        return prefix.isEmpty() ? "(none)" : prefix;
    }

    private static String shownList(List<String> prefixes) {
        return prefixes.isEmpty() ? "-" : prefixes.stream().map(MigrateSubCommand::shown).collect(Collectors.joining(", "));
    }

    /** Source playtime is a delta since the source started empty: add it to what the target has. */
    static List<DatabaseManager.PlayerRow> addPlaytime(List<DatabaseManager.PlayerRow> source,
                                                       List<DatabaseManager.PlayerRow> targetRows) {
        Map<UUID, Long> targetPlaytime = new HashMap<>();
        for (DatabaseManager.PlayerRow t : targetRows) targetPlaytime.put(t.uuid(), t.playtimeSeconds());
        List<DatabaseManager.PlayerRow> out = new ArrayList<>(source.size());
        for (DatabaseManager.PlayerRow r : source) {
            long base = targetPlaytime.getOrDefault(r.uuid(), 0L);
            out.add(new DatabaseManager.PlayerRow(r.uuid(), r.username(), r.firstSeen(), r.lastSeen(),
                    base + r.playtimeSeconds(), r.loginStreak(), r.lastLoginDate(), r.isPrivate(), r.pinnedBadges()));
        }
        return out;
    }

    /**
     * Cumulative achievement counters are added onto the target's progress, capped at the goal.
     * A row that only reaches the goal through the sum is left uncompleted on purpose: the
     * player's next bit of progress completes it in-game, so the notification and rewards are
     * granted normally. "Current value" triggers (login streak, skill levels) and rows the
     * target already completed keep the ordinary highest-wins merge.
     */
    static List<DatabaseManager.AchievementProgressRow> addProgress(List<DatabaseManager.AchievementProgressRow> source,
                                                                    List<DatabaseManager.AchievementProgressRow> targetRows,
                                                                    DPlayerProfiles plugin) {
        Map<String, DatabaseManager.AchievementProgressRow> target = new HashMap<>();
        for (DatabaseManager.AchievementProgressRow t : targetRows) target.put(t.uuid() + "|" + t.achievementId(), t);
        Map<String, AchievementConfig> configs = plugin.getAchievementConfigLoader().getAll();
        List<DatabaseManager.AchievementProgressRow> out = new ArrayList<>(source.size());
        for (DatabaseManager.AchievementProgressRow r : source) {
            DatabaseManager.AchievementProgressRow t = target.get(r.uuid() + "|" + r.achievementId());
            AchievementConfig ac = configs.get(r.achievementId());
            if (t == null || ac == null || t.completedAt() > 0 || isSetSemantics(ac.triggerType)) {
                out.add(r); // nothing to add onto, or not a cumulative counter: plain merge
                continue;
            }
            long merged = t.progress() + r.progress();
            if (ac.triggerCount > 0 && merged > ac.triggerCount) merged = ac.triggerCount;
            out.add(new DatabaseManager.AchievementProgressRow(r.uuid(), r.achievementId(), merged, r.completedAt()));
        }
        return out;
    }

    /** Triggers whose progress is the current value rather than a running total (see AchievementManager#increment). */
    private static boolean isSetSemantics(TriggerType type) {
        return type == TriggerType.LOGIN_STREAK || type == TriggerType.MCMMO_LEVEL_UP
                || type == TriggerType.JOBS_LEVEL_UP || type == TriggerType.AURASKILLS_LEVEL_UP;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            List<String> out = new ArrayList<>();
            if ("none".startsWith(args[1].toLowerCase())) out.add("none");
            if ("add".startsWith(args[1].toLowerCase())) out.add("add");
            return out;
        }
        if (args.length == 3 && "add".startsWith(args[2].toLowerCase())) return List.of("add");
        return List.of();
    }
}
