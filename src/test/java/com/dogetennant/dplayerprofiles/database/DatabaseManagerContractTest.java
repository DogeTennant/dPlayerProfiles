package com.dogetennant.dplayerprofiles.database;

import com.dogetennant.dplayerprofiles.database.DatabaseManager.AchievementProgressRow;
import com.dogetennant.dplayerprofiles.database.DatabaseManager.BadgeRow;
import com.dogetennant.dplayerprofiles.database.DatabaseManager.LeaderboardEntry;
import com.dogetennant.dplayerprofiles.database.DatabaseManager.PlayerRow;
import com.dogetennant.dplayerprofiles.model.AchievementConfig;
import com.dogetennant.dplayerprofiles.model.BadgeConfig;
import com.dogetennant.dplayerprofiles.model.PlayerProfile;
import com.dogetennant.dplayerprofiles.model.StatsSnapshot;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * What both storage backends must do the same way. Runs once per dialect
 * ({@link SQLiteManagerTest}, {@link MySQLManagerTest} on H2, {@link RealMySqlManagerTest} on MySQL 8.4).
 */
abstract class DatabaseManagerContractTest {

    protected static final UUID ALEX = UUID.fromString("00000000-0000-0000-0000-00000000a1e7");
    protected static final UUID STEVE = UUID.fromString("00000000-0000-0000-0000-0000000057e7");
    protected static final UUID BOB = UUID.fromString("00000000-0000-0000-0000-000000000b0b");

    protected DatabaseManager db;

    /** A manager on this test's database with its pool open, tables not created yet. */
    protected abstract DatabaseManager connect(String prefix) throws Exception;

    /** Opens a ready-to-use manager whose tables carry {@code prefix}, in this test's database. */
    protected DatabaseManager open(String prefix) throws Exception {
        DatabaseManager manager = connect(prefix);
        manager.createTables();
        return manager;
    }

    /**
     * Whether this database can run the migration merge statements. H2 cannot (MySQL's
     * {@code IF()} and {@code VALUES()} inside {@code CASE}); {@link RealMySqlManagerTest} runs them
     * on a real MySQL.
     */
    protected boolean runsMergeStatements() {
        return true;
    }

    /** Runs a statement directly in this test's database. */
    protected void execute(String sql) throws Exception {
        try (Connection con = db.getConnection(); Statement stmt = con.createStatement()) {
            stmt.executeUpdate(sql);
        }
    }

    @BeforeEach
    void openDatabase() throws Exception {
        db = open("dpp_");
    }

    @AfterEach
    void closeDatabase() {
        db.shutdown();
    }

    private void player(UUID uuid, String name, long playtime) throws Exception {
        db.upsertPlayer(uuid, name, 1000, 2000, playtime, 1, "2026-10-01");
    }

    //
    // Players
    //

    @Test
    void aPlayerIsReadBackWithEveryField() throws Exception {
        db.upsertPlayer(ALEX, "Alex", 1000, 2000, 3600, 5, "2026-10-01");

        PlayerProfile profile = db.loadPlayer(ALEX);

        assertThat(profile.getUsername()).isEqualTo("Alex");
        assertThat(profile.getFirstSeen()).isEqualTo(1000);
        assertThat(profile.getLastSeen()).isEqualTo(2000);
        assertThat(profile.getPlaytimeSeconds()).isEqualTo(3600);
        assertThat(profile.getLoginStreak()).isEqualTo(5);
        assertThat(profile.getLastLoginDate()).isEqualTo("2026-10-01");
        assertThat(profile.isPrivate()).isFalse();
        assertThat(profile.getPinnedBadges()).isEmpty();
    }

    @Test
    void anUnknownPlayerIsNull() throws Exception {
        assertThat(db.loadPlayer(ALEX)).isNull();
        assertThat(db.loadPlayerByName("Alex")).isNull();
    }

    @Test
    void savingAgainUpdatesEverythingButFirstSeen() throws Exception {
        db.upsertPlayer(ALEX, "Alex", 1000, 2000, 3600, 5, "2026-10-01");

        db.upsertPlayer(ALEX, "Alex_New", 9999, 3000, 7200, 6, "2026-10-02");

        PlayerProfile profile = db.loadPlayer(ALEX);
        assertThat(profile.getUsername()).isEqualTo("Alex_New");
        assertThat(profile.getFirstSeen()).isEqualTo(1000);
        assertThat(profile.getLastSeen()).isEqualTo(3000);
        assertThat(profile.getPlaytimeSeconds()).isEqualTo(7200);
        assertThat(profile.getLoginStreak()).isEqualTo(6);
        assertThat(profile.getLastLoginDate()).isEqualTo("2026-10-02");
    }

    @Test
    void namesAreFoundInAnyLetterCase() throws Exception {
        player(ALEX, "AlexTheGreat", 10);

        assertThat(db.loadPlayerByName("alexthegreat").getUuid()).isEqualTo(ALEX);
        assertThat(db.loadPlayerByName("ALEXTHEGREAT").getUuid()).isEqualTo(ALEX);
    }

    @Test
    void privacyPinnedBadgesPlaytimeAndStreakAreUpdatedOneByOne() throws Exception {
        player(ALEX, "Alex", 10);

        db.setProfilePrivacy(ALEX, true);
        db.setPinnedBadges(ALEX, "veteran,builder,pvp");
        db.updatePlaytime(ALEX, 999);
        db.updateStreak(ALEX, 12, "2026-10-07", 5000);

        PlayerProfile profile = db.loadPlayer(ALEX);
        assertThat(profile.isPrivate()).isTrue();
        assertThat(profile.getPinnedBadges()).containsExactly("veteran", "builder", "pvp");
        assertThat(profile.getPlaytimeSeconds()).isEqualTo(999);
        assertThat(profile.getLoginStreak()).isEqualTo(12);
        assertThat(profile.getLastLoginDate()).isEqualTo("2026-10-07");
        assertThat(profile.getLastSeen()).isEqualTo(5000);
    }

    @Test
    void progressAndBadgesComeWithTheProfile() throws Exception {
        player(ALEX, "Alex", 10);
        db.upsertAchievementProgress(ALEX, "miner", 50, 0);
        db.upsertAchievementProgress(ALEX, "first_join", 1, 1234);
        db.insertBadge(ALEX, "veteran", 777, "Admin");

        PlayerProfile profile = db.loadPlayerByName("alex");

        assertThat(profile.getProgress("miner")).isEqualTo(50);
        assertThat(profile.isCompleted("miner")).isFalse();
        assertThat(profile.isCompleted("first_join")).isTrue();
        assertThat(profile.getBadges()).containsExactlyEntriesOf(Map.of("veteran", 777L));
    }

    @Test
    void deletingAPlayerRemovesTheirProgressAndBadges() throws Exception {
        player(ALEX, "Alex", 10);
        player(STEVE, "Steve", 10);
        db.upsertAchievementProgress(ALEX, "miner", 50, 0);
        db.insertBadge(ALEX, "veteran", 777, "Admin");
        db.insertBadge(STEVE, "veteran", 777, "Admin");

        db.deletePlayer(ALEX);

        assertThat(db.loadPlayer(ALEX)).isNull();
        assertThat(db.dumpAchievementProgress()).isEmpty();
        assertThat(db.dumpBadges()).extracting(BadgeRow::uuid).containsExactly(STEVE);
    }

    //
    // Achievement progress and badges
    //

    @Test
    void aCompletedAchievementStaysCompletedWhenAnOlderWriteArrivesLate() throws Exception {
        player(ALEX, "Alex", 10);
        db.upsertAchievementProgress(ALEX, "miner", 100, 5555);
        db.upsertAchievementProgress(ALEX, "miner", 99, 0);      // written before, landed after

        PlayerProfile profile = db.loadPlayer(ALEX);

        assertThat(profile.isCompleted("miner")).isTrue();
        assertThat(profile.getAchievements().get("miner").completedAt()).isEqualTo(5555);
    }

    @Test
    void offlineTriggersWaitInOrderUntilTheyAreCounted() throws Exception {
        db.addPendingTrigger(ALEX, "TOURNAMENT_WIN", "weekly", 1, 100);
        db.addPendingTrigger(ALEX, "TOURNAMENT_WIN", null, 2, 200);
        db.addPendingTrigger(STEVE, "TOURNAMENT_WIN", "weekly", 1, 300);

        var waiting = db.getPendingTriggers(ALEX);

        assertThat(waiting).extracting(DatabaseManager.PendingTrigger::target, DatabaseManager.PendingTrigger::amount)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("weekly", 1L),
                        org.assertj.core.groups.Tuple.tuple(null, 2L));
        db.deletePendingTriggers(List.of(waiting.get(0).id()));
        assertThat(db.getPendingTriggers(ALEX)).extracting(DatabaseManager.PendingTrigger::amount).containsExactly(2L);
        assertThat(db.getPendingTriggers(STEVE)).hasSize(1);
    }

    @Test
    void deletingAPlayerRemovesTheirOfflineTriggers() throws Exception {
        player(ALEX, "Alex", 10);
        db.addPendingTrigger(ALEX, "TOURNAMENT_WIN", "weekly", 1, 100);

        db.deletePlayer(ALEX);

        assertThat(db.getPendingTriggers(ALEX)).isEmpty();
    }

    @Test
    void offlineTriggersAreCopiedOnceByAMigration() throws Exception {
        db.addPendingTrigger(ALEX, "TOURNAMENT_WIN", "weekly", 1, 100);
        db.addPendingTrigger(ALEX, "TOURNAMENT_WIN", null, 1, 200);
        var exported = db.dumpPendingTriggers();
        DatabaseManager other = open("other_");
        try {
            other.mergePendingTriggers(exported);
            other.mergePendingTriggers(exported);

            assertThat(other.getPendingTriggers(ALEX)).extracting(DatabaseManager.PendingTrigger::createdAt)
                    .containsExactly(100L, 200L);
        } finally {
            other.shutdown();
        }
    }

    @Test
    void progressIsReplacedAndCanBeRemoved() throws Exception {
        player(ALEX, "Alex", 10);
        db.upsertAchievementProgress(ALEX, "miner", 50, 0);
        db.upsertAchievementProgress(ALEX, "miner", 100, 5555);
        db.upsertAchievementProgress(ALEX, "fisher", 3, 0);

        assertThat(db.loadPlayer(ALEX).isCompleted("miner")).isTrue();

        db.deleteAchievementProgress(ALEX, "miner");
        assertThat(db.loadPlayer(ALEX).getAchievements()).containsOnlyKeys("fisher");

        db.deleteAllAchievementProgress(ALEX);
        assertThat(db.loadPlayer(ALEX).getAchievements()).isEmpty();
    }

    @Test
    void aBadgeIsGrantedOnceAndKeepsItsFirstGrant() throws Exception {
        player(ALEX, "Alex", 10);
        db.insertBadge(ALEX, "veteran", 777, "Admin");
        db.insertBadge(ALEX, "veteran", 999, "Console");
        db.insertBadge(ALEX, "builder", 888, "Admin");

        assertThat(db.dumpBadges()).filteredOn(b -> b.badgeId().equals("veteran"))
                .containsExactly(new BadgeRow(ALEX, "veteran", 777, "Admin"));

        db.deleteBadge(ALEX, "veteran");
        assertThat(db.loadPlayer(ALEX).getBadges()).containsOnlyKeys("builder");

        db.deleteAllBadges(ALEX);
        assertThat(db.loadPlayer(ALEX).getBadges()).isEmpty();
    }

    //
    // Leaderboard
    //

    @Test
    void theLeaderboardRanksByCompletedAchievementsThenBadges() throws Exception {
        player(ALEX, "Alex", 10);
        player(STEVE, "Steve", 10);
        player(BOB, "Bob", 10);
        db.upsertAchievementProgress(ALEX, "a", 1, 100);
        db.upsertAchievementProgress(ALEX, "b", 5, 0);          // not completed: does not count
        db.upsertAchievementProgress(STEVE, "a", 1, 100);
        db.insertBadge(STEVE, "veteran", 1, "Admin");
        db.upsertAchievementProgress(BOB, "a", 1, 100);
        db.upsertAchievementProgress(BOB, "b", 1, 100);

        assertThat(db.getLeaderboard(10)).containsExactly(
                new LeaderboardEntry(BOB, "Bob", 2, 0),
                new LeaderboardEntry(STEVE, "Steve", 1, 1),
                new LeaderboardEntry(ALEX, "Alex", 1, 0));
        assertThat(db.getLeaderboard(1)).extracting(LeaderboardEntry::uuid).containsExactly(BOB);
    }

    //
    // Web statistics export
    //

    @Test
    void statsAreReplacedButAMissingBalanceOrNameKeepsTheStoredOne() throws Exception {
        db.upsertPlayerStats(new StatsSnapshot(ALEX, "Alex", 5, 1, 2, 3600, 900, 150.5), 1000);

        db.upsertPlayerStats(new StatsSnapshot(ALEX, null, 6, 1, 3, 3700, 950, null), 2000);

        assertThat(db.getStatsUpdatedAt()).containsExactlyEntriesOf(Map.of(ALEX, 2000L));
        assertThat(db.getStatsUuids(true)).isEmpty();
        try (Connection con = db.getConnection(); Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT username, mob_kills, balance FROM dpp_player_stats")) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getString(1)).isEqualTo("Alex");
            assertThat(rs.getLong(2)).isEqualTo(6);
            assertThat(rs.getDouble(3)).isEqualTo(150.5);
        }
    }

    @Test
    void playersWithoutABalanceCanBeFoundAndFilledIn() throws Exception {
        db.upsertPlayerStats(new StatsSnapshot(ALEX, "Alex", 0, 0, 0, 0, 0, null), 1000);
        db.upsertPlayerStats(new StatsSnapshot(STEVE, "Steve", 0, 0, 0, 0, 0, 10.0), 1000);

        assertThat(db.getStatsUuids(true)).containsExactly(ALEX);
        assertThat(db.getStatsUuids(false)).containsExactlyInAnyOrder(ALEX, STEVE);

        db.updatePlayerBalance(ALEX, 42.0);
        assertThat(db.getStatsUuids(true)).isEmpty();
    }

    @Test
    void lastSeenAndKnownNamesComeFromThePlayersTable() throws Exception {
        player(ALEX, "Alex", 10);
        player(STEVE, "Steve", 10);

        db.updateLastSeen(ALEX, 777_777);

        assertThat(db.loadPlayer(ALEX).getLastSeen()).isEqualTo(777_777);
        assertThat(db.getKnownUsernames()).containsExactlyInAnyOrderEntriesOf(Map.of(ALEX, "Alex", STEVE, "Steve"));
    }

    @Test
    void theCatalogsAreReplacedWithFormattingRemoved() throws Exception {
        AchievementConfig old = new AchievementConfig();
        old.id = "old";
        old.displayName = "Old";
        old.description = "";
        db.replaceAchievementCatalog(List.of(old));

        AchievementConfig miner = new AchievementConfig();
        miner.id = "miner";
        miner.displayName = "<gold>Miner";
        miner.description = "&aMine 100 blocks";
        miner.category = "mining";
        miner.points = 10;
        miner.hidden = true;
        BadgeConfig veteran = new BadgeConfig();
        veteran.id = "veteran";
        veteran.displayName = "<red>Veteran";
        veteran.description = "Played a year";
        db.replaceAchievementCatalog(List.of(miner));
        db.replaceBadgeCatalog(List.of(veteran));

        try (Connection con = db.getConnection(); Statement stmt = con.createStatement()) {
            try (ResultSet rs = stmt.executeQuery("SELECT achievement_id, display_name, description, category, points, hidden, sort_order FROM dpp_achievement_catalog")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getString(1)).isEqualTo("miner");
                assertThat(rs.getString(2)).isEqualTo("Miner");
                assertThat(rs.getString(3)).isEqualTo("Mine 100 blocks");
                assertThat(rs.getString(4)).isEqualTo("mining");
                assertThat(rs.getInt(5)).isEqualTo(10);
                assertThat(rs.getBoolean(6)).isTrue();
                assertThat(rs.getInt(7)).isZero();
                assertThat(rs.next()).isFalse();
            }
            try (ResultSet rs = stmt.executeQuery("SELECT badge_id, display_name FROM dpp_badge_catalog")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getString(2)).isEqualTo("Veteran");
            }
        }
    }

    //
    // Migration between backends: merging never lowers existing data
    //

    @Test
    void mergingPlayersKeepsTheBestOfBoth() throws Exception {
        assumeTrue(runsMergeStatements(), "needs a real MySQL");
        db.upsertPlayer(ALEX, "Alex", 5000, 6000, 100, 9, "2026-10-05");
        db.setPinnedBadges(ALEX, "veteran");

        db.mergePlayers(List.of(
                new PlayerRow(ALEX, "OldAlex", 1000, 2000, 500, 3, "2026-09-01", true, "builder"),
                new PlayerRow(STEVE, "Steve", 1000, 2000, 50, 1, null, false, "")));

        PlayerProfile alex = db.loadPlayer(ALEX);
        assertThat(alex.getUsername()).isEqualTo("Alex");
        assertThat(alex.getFirstSeen()).isEqualTo(1000);           // earliest
        assertThat(alex.getLastSeen()).isEqualTo(6000);            // latest
        assertThat(alex.getPlaytimeSeconds()).isEqualTo(500);      // larger
        assertThat(alex.getLoginStreak()).isEqualTo(9);            // larger
        assertThat(alex.getLastLoginDate()).isEqualTo("2026-10-05"); // later date
        assertThat(alex.isPrivate()).isTrue();                     // private wins
        assertThat(alex.getPinnedBadges()).containsExactly("veteran"); // existing pins kept
        assertThat(db.loadPlayer(STEVE).getUsername()).isEqualTo("Steve");
    }

    @Test
    void aMissingLastLoginDateNeverReplacesAKnownOne() throws Exception {
        assumeTrue(runsMergeStatements(), "needs a real MySQL");
        db.upsertPlayer(ALEX, "Alex", 1000, 2000, 100, 1, null);
        db.upsertPlayer(STEVE, "Steve", 1000, 2000, 100, 1, "2026-10-01");

        db.mergePlayers(List.of(
                new PlayerRow(ALEX, "Alex", 1000, 2000, 100, 1, "2026-09-09", false, ""),
                new PlayerRow(STEVE, "Steve", 1000, 2000, 100, 1, null, false, "")));

        assertThat(db.loadPlayer(ALEX).getLastLoginDate()).isEqualTo("2026-09-09");
        assertThat(db.loadPlayer(STEVE).getLastLoginDate()).isEqualTo("2026-10-01");
    }

    @Test
    void mergingProgressKeepsTheHighestProgressAndTheEarliestCompletion() throws Exception {
        assumeTrue(runsMergeStatements(), "needs a real MySQL");
        player(ALEX, "Alex", 10);
        db.upsertAchievementProgress(ALEX, "miner", 80, 0);
        db.upsertAchievementProgress(ALEX, "fisher", 10, 5000);
        db.upsertAchievementProgress(ALEX, "builder", 10, 3000);

        db.mergeAchievementProgress(List.of(
                new AchievementProgressRow(ALEX, "miner", 100, 4000),    // completed elsewhere
                new AchievementProgressRow(ALEX, "fisher", 2, 0),        // completion is never lost
                new AchievementProgressRow(ALEX, "builder", 10, 1000),   // earlier completion wins
                new AchievementProgressRow(ALEX, "new_one", 7, 0)));

        assertThat(db.dumpAchievementProgress()).containsExactlyInAnyOrder(
                new AchievementProgressRow(ALEX, "miner", 100, 4000),
                new AchievementProgressRow(ALEX, "fisher", 10, 5000),
                new AchievementProgressRow(ALEX, "builder", 10, 1000),
                new AchievementProgressRow(ALEX, "new_one", 7, 0));
    }

    @Test
    void mergingBadgesOnlyAddsMissingOnes() throws Exception {
        assumeTrue(runsMergeStatements(), "needs a real MySQL");
        player(ALEX, "Alex", 10);
        db.insertBadge(ALEX, "veteran", 777, "Admin");

        db.mergeBadges(List.of(new BadgeRow(ALEX, "veteran", 1, "Old"), new BadgeRow(ALEX, "builder", 2, "Old")));

        assertThat(db.dumpBadges()).containsExactlyInAnyOrder(
                new BadgeRow(ALEX, "veteran", 777, "Admin"), new BadgeRow(ALEX, "builder", 2, "Old"));
    }

    @Test
    void mergingTwiceChangesNothingMore() throws Exception {
        assumeTrue(runsMergeStatements(), "needs a real MySQL");
        List<PlayerRow> players = List.of(new PlayerRow(ALEX, "Alex", 1000, 2000, 500, 3, "2026-09-01", false, "veteran"));
        List<AchievementProgressRow> progress = List.of(new AchievementProgressRow(ALEX, "miner", 100, 4000));
        List<BadgeRow> badges = List.of(new BadgeRow(ALEX, "veteran", 1, "Old"));

        for (int i = 0; i < 2; i++) {
            db.mergePlayers(players);
            db.mergeAchievementProgress(progress);
            db.mergeBadges(badges);
        }

        assertThat(db.dumpPlayers()).containsExactlyElementsOf(players);
        assertThat(db.dumpAchievementProgress()).containsExactlyElementsOf(progress);
        assertThat(db.dumpBadges()).containsExactlyElementsOf(badges);
    }

    //
    // Table names
    //

    @Test
    void anotherPluginsPlayersTableIsRefusedWithAHint() throws Exception {
        execute("CREATE TABLE players (id INT, name VARCHAR(16))");

        DatabaseManager unprefixed = connect("");
        try {
            assertThatThrownBy(unprefixed::createTables)
                    .hasMessageContaining("'players' exists but is not a dPlayerProfiles table")
                    .hasMessageContaining("storage.table-prefix");
        } finally {
            unprefixed.shutdown();
        }
    }

    @Test
    void ourTableSetsAreFoundByPrefixAndOthersSkipped() throws Exception {
        execute("CREATE TABLE players (id INT, name VARCHAR(16))");
        DatabaseManager second = open("old_");
        try {
            second.upsertPlayer(ALEX, "Alex", 1, 2, 3, 4, null);
            second.upsertPlayer(STEVE, "Steve", 1, 2, 3, 4, null);

            assertThat(db.detectTablePrefixes()).containsExactlyInAnyOrder("dpp_", "old_");
            assertThat(db.countPlayers("old_")).isEqualTo(2);
            assertThat(db.countPlayers("dpp_")).isZero();
        } finally {
            second.shutdown();
        }
    }

    @Test
    void theTablePrefixCanBeSwitchedToReadAnotherSet() throws Exception {
        DatabaseManager second = open("old_");
        try {
            second.upsertPlayer(ALEX, "Alex", 1, 2, 3, 4, null);

            db.setTablePrefix("old_");

            assertThat(db.getTablePrefix()).isEqualTo("old_");
            assertThat(db.loadPlayer(ALEX)).isNotNull();
        } finally {
            second.shutdown();
        }
    }
}
