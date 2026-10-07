package com.dogetennant.dplayerprofiles.database;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** The contract against the real SQLite file {@code data.db}, plus what only SQLite does. */
class SQLiteManagerTest extends DatabaseManagerContractTest {

    @TempDir
    Path dataFolder;

    @Override
    protected DatabaseManager connect(String prefix) throws Exception {
        TestDatabases.initLogging();
        SQLiteManager manager = new SQLiteManager(dataFolder.toFile(), prefix);
        manager.connect();
        return manager;
    }

    @Test
    void dataSurvivesARestart() throws Exception {
        db.upsertPlayer(ALEX, "Alex", 1, 2, 3600, 4, null);
        db.shutdown();

        DatabaseManager reopened = open("dpp_");
        try {
            assertThat(reopened.loadPlayer(ALEX).getPlaytimeSeconds()).isEqualTo(3600);
        } finally {
            reopened.shutdown();
        }
    }

    @Test
    void anOldPlayersTableGetsTheNewColumns(@TempDir Path oldFolder) throws Exception {
        UUID old = UUID.randomUUID();
        try (Connection con = DriverManager.getConnection("jdbc:sqlite:" + oldFolder.resolve("data.db"));
             Statement stmt = con.createStatement()) {
            stmt.executeUpdate("CREATE TABLE dpp_players (player_uuid TEXT PRIMARY KEY, username TEXT NOT NULL, "
                    + "first_seen INTEGER NOT NULL, last_seen INTEGER NOT NULL, playtime_seconds INTEGER NOT NULL DEFAULT 0, "
                    + "login_streak INTEGER NOT NULL DEFAULT 0, last_login_date TEXT)");
            stmt.executeUpdate("INSERT INTO dpp_players VALUES ('" + old + "', 'Veteran', 1, 2, 3, 4, NULL)");
        }

        DatabaseManager upgraded = TestDatabases.sqlite(oldFolder, "dpp_");
        try {
            assertThat(upgraded.loadPlayer(old).isPrivate()).isFalse();
            assertThat(upgraded.loadPlayer(old).getPinnedBadges()).isEmpty();
            upgraded.setProfilePrivacy(old, true);
            assertThat(upgraded.loadPlayer(old).isPrivate()).isTrue();
        } finally {
            upgraded.shutdown();
        }
    }
}
