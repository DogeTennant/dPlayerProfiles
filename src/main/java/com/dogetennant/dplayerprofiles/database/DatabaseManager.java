package com.dogetennant.dplayerprofiles.database;

import com.dogetennant.dplayerprofiles.model.AchievementConfig;
import com.dogetennant.dplayerprofiles.model.BadgeConfig;
import com.dogetennant.dplayerprofiles.model.PlayerProfile;
import com.dogetennant.dplayerprofiles.model.StatsSnapshot;
import com.dogetennant.dplayerprofiles.util.ColorUtil;
import com.dogetennant.dplayerprofiles.util.LogUtil;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public abstract class DatabaseManager {

    protected HikariDataSource dataSource;
    protected String prefix;

    protected DatabaseManager(String tablePrefix) {
        this.prefix = tablePrefix;
    }

    /** Opens the connection pool and creates or upgrades the tables. */
    public void initialize() throws SQLException {
        connect();
        createTables();
    }

    /** Opens the connection pool only - the schema is left untouched. Used to read a migration source. */
    public abstract void connect() throws SQLException;

    /**
     * Makes sure '<prefix>players' is this plugin's table and not one another plugin created
     * under the same name (an unprefixed 'players' is a popular table name). Called right after
     * the CREATE TABLE IF NOT EXISTS statements, before anything is altered or written.
     */
    protected void verifyPlayersTable(Statement stmt) throws SQLException {
        try {
            stmt.executeQuery("SELECT player_uuid, login_streak FROM " + t("players") + " WHERE 1=0").close();
        } catch (SQLException e) {
            throw new SQLException("'" + t("players") + "' exists but is not a dPlayerProfiles table - another plugin"
                    + " probably owns it. Set storage.table-prefix in config.yml (e.g. 'dpp_') so this plugin's"
                    + " tables get their own names.", e);
        }
    }

    protected abstract void createTables() throws SQLException;

    /**
     * Changes which table set this manager reads. Only meant for migration, where the other
     * backend's tables may carry a different prefix than the one configured today.
     */
    public void setTablePrefix(String tablePrefix) {
        this.prefix = tablePrefix;
    }

    public String getTablePrefix() {
        return prefix;
    }

    /** Names of all tables in the connected database. */
    protected abstract List<String> listTables() throws SQLException;

    /**
     * Prefixes of every dPlayerProfiles table set in this database, found by looking for
     * '<prefix>players' tables that have the plugin's columns (other plugins have 'players' too).
     */
    public List<String> detectTablePrefixes() throws SQLException {
        List<String> prefixes = new ArrayList<>();
        for (String table : listTables()) {
            if (!table.endsWith("players")) continue;
            String candidate = table.substring(0, table.length() - "players".length());
            try (Connection con = getConnection(); Statement stmt = con.createStatement()) {
                stmt.executeQuery("SELECT player_uuid, login_streak FROM " + table + " WHERE 1=0").close();
                prefixes.add(candidate);
            } catch (SQLException notOurs) { /* a 'players' table from some other plugin */ }
        }
        return prefixes;
    }

    /** Number of profiles in the '<prefix>players' table of the given prefix. */
    public long countPlayers(String tablePrefix) throws SQLException {
        try (Connection con = getConnection(); Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM " + tablePrefix + "players")) {
            return rs.next() ? rs.getLong(1) : 0;
        }
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            LogUtil.info("Database connection pool closed.");
        }
    }

    protected String t(String table) {
        return prefix + table;
    }

    //  Players 

    public abstract void upsertPlayer(UUID uuid, String username, long firstSeen,
                                       long lastSeen, long playtimeSeconds,
                                       int loginStreak, String lastLoginDate) throws SQLException;

    public abstract PlayerProfile loadPlayer(UUID uuid) throws SQLException;

    /** Case-insensitive username lookup. Returns null if no matching player found. */
    public abstract PlayerProfile loadPlayerByName(String name) throws SQLException;

    public abstract void setProfilePrivacy(UUID uuid, boolean isPrivate) throws SQLException;

    public abstract void setPinnedBadges(UUID uuid, String encoded) throws SQLException;

    public abstract void updatePlaytime(UUID uuid, long playtimeSeconds) throws SQLException;

    public abstract void updateStreak(UUID uuid, int streak, String lastLoginDate,
                                       long lastSeen) throws SQLException;

    public abstract void deletePlayer(UUID uuid) throws SQLException;

    //  Achievement progress 

    public abstract void upsertAchievementProgress(UUID uuid, String achievementId,
                                                    long progress, long completedAt) throws SQLException;

    public abstract void deleteAchievementProgress(UUID uuid, String achievementId) throws SQLException;

    public abstract void deleteAllAchievementProgress(UUID uuid) throws SQLException;

    //  Badges 

    public abstract void insertBadge(UUID uuid, String badgeId,
                                      long grantedAt, String grantedBy) throws SQLException;

    public abstract void deleteBadge(UUID uuid, String badgeId) throws SQLException;

    public abstract void deleteAllBadges(UUID uuid) throws SQLException;

    //  Leaderboard 

    /** Returns a list of [uuid, username, completedAchievements] for the top N players. */
    public abstract List<LeaderboardEntry> getLeaderboard(int limit) throws SQLException;

    public record LeaderboardEntry(UUID uuid, String username, int completedAchievements, int badges) {}

    //  Web statistics export (see stats.WebStatsExporter)

    /** Inserts or updates a player's exported stats. A null balance keeps the stored one. */
    public abstract void upsertPlayerStats(StatsSnapshot snapshot, long updatedAt) throws SQLException;

    /** Sets only the last_seen column (a player vanished or reappeared). */
    public void updateLastSeen(UUID uuid, long lastSeen) throws SQLException {
        String sql = "UPDATE " + t("players") + " SET last_seen=? WHERE player_uuid=?";
        try (Connection con = getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, lastSeen);
            ps.setString(2, uuid.toString());
            ps.executeUpdate();
        }
    }

    /** Sets only the balance column, e.g. from the offline balance backfill. */
    public void updatePlayerBalance(UUID uuid, double balance) throws SQLException {
        String sql = "UPDATE " + t("player_stats") + " SET balance=? WHERE player_uuid=?";
        try (Connection con = getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setDouble(1, balance);
            ps.setString(2, uuid.toString());
            ps.executeUpdate();
        }
    }

    /** uuid -> updated_at (epoch ms) for every row in player_stats. */
    public Map<UUID, Long> getStatsUpdatedAt() throws SQLException {
        Map<UUID, Long> result = new HashMap<>();
        String sql = "SELECT player_uuid, updated_at FROM " + t("player_stats");
        try (Connection con = getConnection(); Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                result.put(UUID.fromString(rs.getString(1)), rs.getLong(2));
            }
        }
        return result;
    }

    /** UUIDs present in player_stats; optionally only those whose balance was never read. */
    public List<UUID> getStatsUuids(boolean onlyWithoutBalance) throws SQLException {
        List<UUID> result = new ArrayList<>();
        String sql = "SELECT player_uuid FROM " + t("player_stats")
                + (onlyWithoutBalance ? " WHERE balance IS NULL" : "");
        try (Connection con = getConnection(); Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                result.add(UUID.fromString(rs.getString(1)));
            }
        }
        return result;
    }

    /** uuid -> username for every player the plugin has a profile for. */
    public Map<UUID, String> getKnownUsernames() throws SQLException {
        Map<UUID, String> result = new HashMap<>();
        String sql = "SELECT player_uuid, username FROM " + t("players");
        try (Connection con = getConnection(); Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                result.put(UUID.fromString(rs.getString(1)), rs.getString(2));
            }
        }
        return result;
    }

    /** Replaces the exported achievement definitions (names/descriptions stripped of formatting). */
    public void replaceAchievementCatalog(Collection<AchievementConfig> achievements) throws SQLException {
        String insert = "INSERT INTO " + t("achievement_catalog")
                + " (achievement_id, display_name, description, category, points, hidden, icon, sort_order)"
                + " VALUES (?,?,?,?,?,?,?,?)";
        try (Connection con = getConnection()) {
            con.setAutoCommit(false);
            try (Statement stmt = con.createStatement()) {
                stmt.executeUpdate("DELETE FROM " + t("achievement_catalog"));
            }
            try (PreparedStatement ps = con.prepareStatement(insert)) {
                int order = 0;
                for (AchievementConfig a : achievements) {
                    ps.setString(1, a.id);
                    ps.setString(2, ColorUtil.stripFormatting(a.displayName));
                    ps.setString(3, ColorUtil.stripFormatting(a.description));
                    ps.setString(4, a.category);
                    ps.setInt(5, a.points);
                    ps.setBoolean(6, a.hidden);
                    ps.setString(7, a.icon == null ? null : a.icon.name());
                    ps.setInt(8, order++);
                    ps.addBatch();
                }
                ps.executeBatch();
            }
            con.commit();
        }
    }

    /** Replaces the exported badge definitions. */
    public void replaceBadgeCatalog(Collection<BadgeConfig> badges) throws SQLException {
        String insert = "INSERT INTO " + t("badge_catalog")
                + " (badge_id, display_name, description, icon) VALUES (?,?,?,?)";
        try (Connection con = getConnection()) {
            con.setAutoCommit(false);
            try (Statement stmt = con.createStatement()) {
                stmt.executeUpdate("DELETE FROM " + t("badge_catalog"));
            }
            try (PreparedStatement ps = con.prepareStatement(insert)) {
                for (BadgeConfig b : badges) {
                    ps.setString(1, b.id);
                    ps.setString(2, ColorUtil.stripFormatting(b.displayName));
                    ps.setString(3, ColorUtil.stripFormatting(b.description));
                    ps.setString(4, b.icon == null ? null : b.icon.name());
                    ps.addBatch();
                }
                ps.executeBatch();
            }
            con.commit();
        }
    }

    //  Triggers waiting for an offline player

    /** A trigger that happened while the player was offline, counted at their next login. */
    public record PendingTrigger(long id, UUID uuid, String triggerType, String target, long amount, long createdAt) {}

    public void addPendingTrigger(UUID uuid, String triggerType, String target, long amount, long createdAt)
            throws SQLException {
        String sql = "INSERT INTO " + t("pending_triggers")
                + " (player_uuid, trigger_type, target, amount, created_at) VALUES (?,?,?,?,?)";
        try (Connection con = getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, triggerType);
            ps.setString(3, target);
            ps.setLong(4, amount);
            ps.setLong(5, createdAt);
            ps.executeUpdate();
        }
    }

    /** The player's waiting triggers, oldest first. */
    public List<PendingTrigger> getPendingTriggers(UUID uuid) throws SQLException {
        List<PendingTrigger> rows = new ArrayList<>();
        String sql = "SELECT id, trigger_type, target, amount, created_at FROM " + t("pending_triggers")
                + " WHERE player_uuid=? ORDER BY id";
        try (Connection con = getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new PendingTrigger(rs.getLong("id"), uuid, rs.getString("trigger_type"),
                            rs.getString("target"), rs.getLong("amount"), rs.getLong("created_at")));
                }
            }
        }
        return rows;
    }

    /** Removes triggers once they are counted. */
    public void deletePendingTriggers(List<Long> ids) throws SQLException {
        if (ids.isEmpty()) return;
        String sql = "DELETE FROM " + t("pending_triggers") + " WHERE id=?";
        try (Connection con = getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            for (long id : ids) {
                ps.setLong(1, id);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    protected void deleteAllPendingTriggers(UUID uuid) throws SQLException {
        String sql = "DELETE FROM " + t("pending_triggers") + " WHERE player_uuid=?";
        try (Connection con = getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
        }
    }

    //  Migration between backends (see command.subcommand.MigrateSubCommand)

    public record PlayerRow(UUID uuid, String username, long firstSeen, long lastSeen,
                            long playtimeSeconds, int loginStreak, String lastLoginDate,
                            boolean isPrivate, String pinnedBadges) {}

    public record AchievementProgressRow(UUID uuid, String achievementId, long progress, long completedAt) {}

    public record BadgeRow(UUID uuid, String badgeId, long grantedAt, String grantedBy) {}

    public List<PlayerRow> dumpPlayers() throws SQLException {
        List<PlayerRow> rows = new ArrayList<>();
        String sql = "SELECT player_uuid, username, first_seen, last_seen, playtime_seconds, login_streak,"
                + " last_login_date, is_private, pinned_badges FROM " + t("players");
        try (Connection con = getConnection(); Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                rows.add(new PlayerRow(UUID.fromString(rs.getString("player_uuid")),
                        rs.getString("username"), rs.getLong("first_seen"), rs.getLong("last_seen"),
                        rs.getLong("playtime_seconds"), rs.getInt("login_streak"),
                        rs.getString("last_login_date"), rs.getBoolean("is_private"),
                        rs.getString("pinned_badges")));
            }
        }
        return rows;
    }

    public List<AchievementProgressRow> dumpAchievementProgress() throws SQLException {
        List<AchievementProgressRow> rows = new ArrayList<>();
        String sql = "SELECT player_uuid, achievement_id, progress, completed_at FROM " + t("achievement_progress");
        try (Connection con = getConnection(); Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                rows.add(new AchievementProgressRow(UUID.fromString(rs.getString("player_uuid")),
                        rs.getString("achievement_id"), rs.getLong("progress"), rs.getLong("completed_at")));
            }
        }
        return rows;
    }

    public List<BadgeRow> dumpBadges() throws SQLException {
        List<BadgeRow> rows = new ArrayList<>();
        String sql = "SELECT player_uuid, badge_id, granted_at, granted_by FROM " + t("badges");
        try (Connection con = getConnection(); Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                rows.add(new BadgeRow(UUID.fromString(rs.getString("player_uuid")),
                        rs.getString("badge_id"), rs.getLong("granted_at"), rs.getString("granted_by")));
            }
        }
        return rows;
    }

    /** Every waiting trigger; none when the source is older than the table (1.1.0). */
    public List<PendingTrigger> dumpPendingTriggers() throws SQLException {
        boolean exists = listTables().stream().anyMatch(table -> table.equalsIgnoreCase(t("pending_triggers")));
        if (!exists) return List.of();
        List<PendingTrigger> rows = new ArrayList<>();
        String sql = "SELECT id, player_uuid, trigger_type, target, amount, created_at FROM " + t("pending_triggers")
                + " ORDER BY id";
        try (Connection con = getConnection(); Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                rows.add(new PendingTrigger(rs.getLong("id"), UUID.fromString(rs.getString("player_uuid")),
                        rs.getString("trigger_type"), rs.getString("target"), rs.getLong("amount"),
                        rs.getLong("created_at")));
            }
        }
        return rows;
    }

    /** Adds waiting triggers that are not there yet (same player, trigger, target, amount and time). */
    public void mergePendingTriggers(List<PendingTrigger> rows) throws SQLException {
        String exists = "SELECT 1 FROM " + t("pending_triggers") + " WHERE player_uuid=? AND trigger_type=?"
                + " AND (target=? OR (target IS NULL AND ? IS NULL)) AND amount=? AND created_at=?";
        for (PendingTrigger row : rows) {
            try (Connection con = getConnection(); PreparedStatement ps = con.prepareStatement(exists)) {
                ps.setString(1, row.uuid().toString());
                ps.setString(2, row.triggerType());
                ps.setString(3, row.target());
                ps.setString(4, row.target());
                ps.setLong(5, row.amount());
                ps.setLong(6, row.createdAt());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) continue;
                }
            }
            addPendingTrigger(row.uuid(), row.triggerType(), row.target(), row.amount(), row.createdAt());
        }
    }

    /**
     * Merges rows exported from another backend without ever lowering existing data:
     * the larger playtime/streak/progress wins, first_seen keeps the earliest, last_seen
     * the latest, and an existing completion date is never lost. Safe to run repeatedly.
     */
    public abstract void mergePlayers(List<PlayerRow> rows) throws SQLException;

    public abstract void mergeAchievementProgress(List<AchievementProgressRow> rows) throws SQLException;

    /** Inserts badges that are missing; existing ones are left untouched. */
    public abstract void mergeBadges(List<BadgeRow> rows) throws SQLException;
}
