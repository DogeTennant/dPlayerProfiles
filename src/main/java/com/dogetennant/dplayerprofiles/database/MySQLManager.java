package com.dogetennant.dplayerprofiles.database;

import com.dogetennant.dplayerprofiles.config.MainConfig;
import com.dogetennant.dplayerprofiles.model.PlayerProfile;
import com.dogetennant.dplayerprofiles.util.LogUtil;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MySQLManager extends DatabaseManager {

    private final MainConfig config;

    public MySQLManager(MainConfig config) {
        super(config.tablePrefix);
        this.config = config;
    }

    @Override
    public void initialize() throws SQLException {
        HikariConfig hc = new HikariConfig();
        hc.setJdbcUrl("jdbc:mysql://" + config.mysqlHost + ":" + config.mysqlPort
                + "/" + config.mysqlDatabase
                + "?useSSL=false&characterEncoding=utf8mb4&useUnicode=true");
        hc.setUsername(config.mysqlUsername);
        hc.setPassword(config.mysqlPassword);
        hc.setMaximumPoolSize(config.mysqlPoolSize);
        hc.setConnectionTimeout(config.mysqlConnectionTimeout);
        hc.setMaxLifetime(config.mysqlMaxLifetime);
        hc.setPoolName("dPlayerProfiles-Pool");
        dataSource = new HikariDataSource(hc);

        createTables();
        LogUtil.info("MySQL database initialised at " + config.mysqlHost + "/" + config.mysqlDatabase);
    }

    private void createTables() throws SQLException {
        try (Connection con = getConnection(); Statement stmt = con.createStatement()) {
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS %s (
                    player_uuid      VARCHAR(36)  PRIMARY KEY,
                    username         VARCHAR(16)  NOT NULL,
                    first_seen       BIGINT       NOT NULL,
                    last_seen        BIGINT       NOT NULL,
                    playtime_seconds BIGINT       NOT NULL DEFAULT 0,
                    login_streak     INT          NOT NULL DEFAULT 0,
                    last_login_date  VARCHAR(10)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4""".formatted(t("players")));

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS %s (
                    player_uuid    VARCHAR(36)  NOT NULL,
                    achievement_id VARCHAR(128) NOT NULL,
                    progress       BIGINT       NOT NULL DEFAULT 0,
                    completed_at   BIGINT       NOT NULL DEFAULT 0,
                    PRIMARY KEY (player_uuid, achievement_id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4""".formatted(t("achievement_progress")));

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS %s (
                    player_uuid VARCHAR(36)  NOT NULL,
                    badge_id    VARCHAR(128) NOT NULL,
                    granted_at  BIGINT       NOT NULL,
                    granted_by  VARCHAR(36),
                    PRIMARY KEY (player_uuid, badge_id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4""".formatted(t("badges")));

            // Migrations for existing tables
            try {
                stmt.executeUpdate("ALTER TABLE " + t("players")
                        + " ADD COLUMN is_private TINYINT(1) NOT NULL DEFAULT 0");
            } catch (SQLException ignored) { /* column already exists */ }
            try {
                stmt.executeUpdate("ALTER TABLE " + t("players")
                        + " ADD COLUMN pinned_badges VARCHAR(400) NOT NULL DEFAULT ''");
            } catch (SQLException ignored) { /* column already exists */ }
        }
    }

    @Override
    public void upsertPlayer(UUID uuid, String username, long firstSeen, long lastSeen,
                              long playtimeSeconds, int loginStreak, String lastLoginDate) throws SQLException {
        String sql = "INSERT INTO " + t("players")
                + " (player_uuid, username, first_seen, last_seen, playtime_seconds, login_streak, last_login_date)"
                + " VALUES (?,?,?,?,?,?,?)"
                + " ON DUPLICATE KEY UPDATE username=VALUES(username), last_seen=VALUES(last_seen),"
                + " playtime_seconds=VALUES(playtime_seconds), login_streak=VALUES(login_streak),"
                + " last_login_date=VALUES(last_login_date)";
        try (Connection con = getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, username);
            ps.setLong(3, firstSeen);
            ps.setLong(4, lastSeen);
            ps.setLong(5, playtimeSeconds);
            ps.setInt(6, loginStreak);
            ps.setString(7, lastLoginDate);
            ps.executeUpdate();
        }
    }

    @Override
    public PlayerProfile loadPlayer(UUID uuid) throws SQLException {
        String sql = "SELECT username, first_seen, last_seen, playtime_seconds, login_streak, last_login_date, is_private, pinned_badges FROM "
                + t("players") + " WHERE player_uuid=?";
        PlayerProfile profile = null;
        try (Connection con = getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    profile = new PlayerProfile(uuid,
                            rs.getString("username"),
                            rs.getLong("first_seen"),
                            rs.getLong("last_seen"),
                            rs.getLong("playtime_seconds"),
                            rs.getInt("login_streak"),
                            rs.getString("last_login_date"));
                    profile.setPrivate(rs.getBoolean("is_private"));
                    profile.setPinnedBadges(parsePinnedBadges(rs.getString("pinned_badges")));
                }
            }
        }
        if (profile == null) return null;

        String achSql = "SELECT achievement_id, progress, completed_at FROM "
                + t("achievement_progress") + " WHERE player_uuid=?";
        try (Connection con = getConnection(); PreparedStatement ps = con.prepareStatement(achSql)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    profile.setProgress(rs.getString("achievement_id"),
                            rs.getLong("progress"), rs.getLong("completed_at"));
                }
            }
        }

        String badgeSql = "SELECT badge_id, granted_at FROM " + t("badges") + " WHERE player_uuid=?";
        try (Connection con = getConnection(); PreparedStatement ps = con.prepareStatement(badgeSql)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    profile.grantBadge(rs.getString("badge_id"), rs.getLong("granted_at"));
                }
            }
        }

        return profile;
    }

    @Override
    public PlayerProfile loadPlayerByName(String name) throws SQLException {
        String sql = "SELECT player_uuid, username, first_seen, last_seen, playtime_seconds, login_streak, last_login_date, is_private, pinned_badges FROM "
                + t("players") + " WHERE LOWER(username)=LOWER(?) LIMIT 1";
        UUID uuid = null;
        PlayerProfile profile = null;
        try (Connection con = getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    uuid = UUID.fromString(rs.getString("player_uuid"));
                    profile = new PlayerProfile(uuid,
                            rs.getString("username"),
                            rs.getLong("first_seen"),
                            rs.getLong("last_seen"),
                            rs.getLong("playtime_seconds"),
                            rs.getInt("login_streak"),
                            rs.getString("last_login_date"));
                    profile.setPrivate(rs.getBoolean("is_private"));
                    profile.setPinnedBadges(parsePinnedBadges(rs.getString("pinned_badges")));
                }
            }
        }
        if (profile == null) return null;

        String achSql = "SELECT achievement_id, progress, completed_at FROM "
                + t("achievement_progress") + " WHERE player_uuid=?";
        try (Connection con = getConnection(); PreparedStatement ps = con.prepareStatement(achSql)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    profile.setProgress(rs.getString("achievement_id"),
                            rs.getLong("progress"), rs.getLong("completed_at"));
                }
            }
        }

        String badgeSql = "SELECT badge_id, granted_at FROM " + t("badges") + " WHERE player_uuid=?";
        try (Connection con = getConnection(); PreparedStatement ps = con.prepareStatement(badgeSql)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    profile.grantBadge(rs.getString("badge_id"), rs.getLong("granted_at"));
                }
            }
        }

        return profile;
    }

    @Override
    public void setProfilePrivacy(UUID uuid, boolean isPrivate) throws SQLException {
        String sql = "UPDATE " + t("players") + " SET is_private=? WHERE player_uuid=?";
        try (Connection con = getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setBoolean(1, isPrivate);
            ps.setString(2, uuid.toString());
            ps.executeUpdate();
        }
    }

    @Override
    public void setPinnedBadges(UUID uuid, String encoded) throws SQLException {
        String sql = "UPDATE " + t("players") + " SET pinned_badges=? WHERE player_uuid=?";
        try (Connection con = getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, encoded == null ? "" : encoded);
            ps.setString(2, uuid.toString());
            ps.executeUpdate();
        }
    }

    private static java.util.List<String> parsePinnedBadges(String encoded) {
        if (encoded == null || encoded.isBlank()) return new java.util.ArrayList<>();
        return new java.util.ArrayList<>(java.util.Arrays.asList(encoded.split(",", 3)));
    }

    @Override
    public void updatePlaytime(UUID uuid, long playtimeSeconds) throws SQLException {
        String sql = "UPDATE " + t("players") + " SET playtime_seconds=? WHERE player_uuid=?";
        try (Connection con = getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, playtimeSeconds);
            ps.setString(2, uuid.toString());
            ps.executeUpdate();
        }
    }

    @Override
    public void updateStreak(UUID uuid, int streak, String lastLoginDate, long lastSeen) throws SQLException {
        String sql = "UPDATE " + t("players")
                + " SET login_streak=?, last_login_date=?, last_seen=? WHERE player_uuid=?";
        try (Connection con = getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, streak);
            ps.setString(2, lastLoginDate);
            ps.setLong(3, lastSeen);
            ps.setString(4, uuid.toString());
            ps.executeUpdate();
        }
    }

    @Override
    public void deletePlayer(UUID uuid) throws SQLException {
        deleteAllAchievementProgress(uuid);
        deleteAllBadges(uuid);
        String sql = "DELETE FROM " + t("players") + " WHERE player_uuid=?";
        try (Connection con = getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
        }
    }

    @Override
    public void upsertAchievementProgress(UUID uuid, String achievementId,
                                           long progress, long completedAt) throws SQLException {
        String sql = "INSERT INTO " + t("achievement_progress")
                + " (player_uuid, achievement_id, progress, completed_at) VALUES (?,?,?,?)"
                + " ON DUPLICATE KEY UPDATE progress=VALUES(progress), completed_at=VALUES(completed_at)";
        try (Connection con = getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, achievementId);
            ps.setLong(3, progress);
            ps.setLong(4, completedAt);
            ps.executeUpdate();
        }
    }

    @Override
    public void deleteAchievementProgress(UUID uuid, String achievementId) throws SQLException {
        String sql = "DELETE FROM " + t("achievement_progress")
                + " WHERE player_uuid=? AND achievement_id=?";
        try (Connection con = getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, achievementId);
            ps.executeUpdate();
        }
    }

    @Override
    public void deleteAllAchievementProgress(UUID uuid) throws SQLException {
        String sql = "DELETE FROM " + t("achievement_progress") + " WHERE player_uuid=?";
        try (Connection con = getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
        }
    }

    @Override
    public void insertBadge(UUID uuid, String badgeId, long grantedAt, String grantedBy) throws SQLException {
        String sql = "INSERT IGNORE INTO " + t("badges")
                + " (player_uuid, badge_id, granted_at, granted_by) VALUES (?,?,?,?)";
        try (Connection con = getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, badgeId);
            ps.setLong(3, grantedAt);
            ps.setString(4, grantedBy);
            ps.executeUpdate();
        }
    }

    @Override
    public void deleteBadge(UUID uuid, String badgeId) throws SQLException {
        String sql = "DELETE FROM " + t("badges") + " WHERE player_uuid=? AND badge_id=?";
        try (Connection con = getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, badgeId);
            ps.executeUpdate();
        }
    }

    @Override
    public void deleteAllBadges(UUID uuid) throws SQLException {
        String sql = "DELETE FROM " + t("badges") + " WHERE player_uuid=?";
        try (Connection con = getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
        }
    }

    @Override
    public List<LeaderboardEntry> getLeaderboard(int limit) throws SQLException {
        String sql = """
            SELECT p.player_uuid, p.username,
                COALESCE(ac.completed, 0) AS completed,
                COALESCE(b.badge_count, 0) AS badge_count
            FROM %s p
            LEFT JOIN (
                SELECT player_uuid, COUNT(*) AS completed
                FROM %s WHERE completed_at > 0
                GROUP BY player_uuid
            ) ac ON p.player_uuid = ac.player_uuid
            LEFT JOIN (
                SELECT player_uuid, COUNT(*) AS badge_count
                FROM %s
                GROUP BY player_uuid
            ) b ON p.player_uuid = b.player_uuid
            ORDER BY completed DESC, badge_count DESC
            LIMIT ?
            """.formatted(t("players"), t("achievement_progress"), t("badges"));
        List<LeaderboardEntry> result = new ArrayList<>();
        try (Connection con = getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new LeaderboardEntry(
                            UUID.fromString(rs.getString("player_uuid")),
                            rs.getString("username"),
                            rs.getInt("completed"),
                            rs.getInt("badge_count")));
                }
            }
        }
        return result;
    }
}
