package com.dogetennant.dplayerprofiles.config;

import java.util.Collections;
import java.util.Map;

public class MainConfig {

    // storage
    public String storageType;
    public String tablePrefix;

    // mysql
    public String mysqlHost;
    public int mysqlPort;
    public String mysqlDatabase;
    public String mysqlUsername;
    public String mysqlPassword;
    public int mysqlPoolSize;
    public long mysqlConnectionTimeout;
    public long mysqlMaxLifetime;

    // general
    public String language;
    public boolean streaksEnabled;
    public int leaderboardSize;
    public int playtimeUpdateInterval;
    public boolean playtimeIgnoreVanished;
    public boolean lastSeenIgnoreVanished;
    public boolean loginIgnoreVanished;

    // anti-afk
    public boolean antiAfkEnabled;
    public int antiAfkTimeout; // seconds

    // anti-farm
    public boolean antiFarmEnabled;
    public int antiFarmMaxPerChunk; // cap on tracked positions per chunk

    // notifications
    public String achievementNotification; // title, chat, both

    // category-name -> required permission node (empty map = no restrictions)
    public Map<String, String> categoryPermissions = Collections.emptyMap();

    // points system
    public boolean pointsEnabled;
    public int pointsPerNode;            // points needed to advance one node
    public int pointsNodeCount;          // total number of nodes in the path
    public int pointsMilestoneInterval;  // every Nth node uses the milestone block (0 = disabled)

    // web statistics export
    public boolean webStatsEnabled;
    public int webStatsInterval;              // seconds between snapshots of online players
    public boolean webStatsBackfillOnStartup;
    public boolean webStatsBackfillBalances;
    public int webStatsBackfillBalancesPerTick;
    public boolean webStatsExportCatalog;
}
