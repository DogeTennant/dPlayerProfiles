package com.dogetennant.dplayerprofiles.database;

import com.dogetennant.dplayerprofiles.model.PlayerProfile;
import com.dogetennant.dplayerprofiles.util.LogUtil;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public abstract class DatabaseManager {

    protected HikariDataSource dataSource;
    protected final String prefix;

    protected DatabaseManager(String tablePrefix) {
        this.prefix = tablePrefix;
    }

    public abstract void initialize() throws SQLException;

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
}
