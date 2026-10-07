package com.dogetennant.dplayerprofiles.database;

import com.dogetennant.dplayerprofiles.config.MainConfig;
import com.dogetennant.dplayerprofiles.database.DatabaseManager.AchievementProgressRow;
import com.dogetennant.dplayerprofiles.database.DatabaseManager.BadgeRow;
import com.dogetennant.dplayerprofiles.model.PlayerProfile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The whole contract - including the migration merges H2 cannot parse - against a real MySQL 8.4
 * in Docker, through the plugin's own {@code connect()} (Hikari + Connector/J). Skipped where
 * Docker is not available. Every test gets its own database.
 */
@Testcontainers(disabledWithoutDocker = true)
class RealMySqlManagerTest extends DatabaseManagerContractTest {

    /** Data in memory and no disk syncs: a throwaway database, and DDL is slow on Docker's disk. */
    @Container
    private static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.4")
            .withTmpFs(Map.of("/var/lib/mysql", "rw"))
            .withCommand("--innodb-flush-log-at-trx-commit=0", "--sync-binlog=0", "--skip-log-bin",
                    "--innodb-doublewrite=OFF", "--innodb-flush-method=nosync");

    /** This test's database, created on first use. */
    private String database;

    @TempDir
    Path dataFolder;

    @Override
    protected DatabaseManager connect(String prefix) throws Exception {
        if (database == null) {
            database = "t" + UUID.randomUUID().toString().replace("-", "");
            // root over a URL that may fetch the server key; afterwards the plugin's URL logs in
            // with the cached credentials
            try (Connection con = DriverManager.getConnection(
                    "jdbc:mysql://" + MYSQL.getHost() + ":" + MYSQL.getMappedPort(3306)
                            + "/?useSSL=false&allowPublicKeyRetrieval=true", "root", MYSQL.getPassword());
                 Statement stmt = con.createStatement()) {
                stmt.executeUpdate("CREATE DATABASE " + database);
            }
        }
        TestDatabases.initLogging();
        MainConfig config = new MainConfig();
        config.tablePrefix = prefix;
        config.mysqlHost = MYSQL.getHost();
        config.mysqlPort = MYSQL.getMappedPort(3306);
        config.mysqlDatabase = database;
        config.mysqlUsername = "root";
        config.mysqlPassword = MYSQL.getPassword();
        config.mysqlPoolSize = 2;
        config.mysqlConnectionTimeout = 10_000;
        config.mysqlMaxLifetime = 1_800_000;
        MySQLManager manager = new MySQLManager(config);
        manager.connect();
        return manager;
    }

    /** What /dpp migrate does: dump SQLite, merge into MySQL (different prefixes), twice. */
    @Test
    void migratingFromSqliteBringsEverythingAndARerunChangesNothing() throws Exception {
        SQLiteManager sqlite = TestDatabases.sqlite(dataFolder, "");
        try {
            sqlite.upsertPlayer(ALEX, "Alex", 1000, 2000, 3600, 5, "2026-10-01");
            sqlite.setPinnedBadges(ALEX, "veteran");
            sqlite.upsertAchievementProgress(ALEX, "miner", 100, 4000);
            sqlite.insertBadge(ALEX, "veteran", 777, "Admin");

            for (int run = 0; run < 2; run++) {
                db.mergePlayers(sqlite.dumpPlayers());
                db.mergeAchievementProgress(sqlite.dumpAchievementProgress());
                db.mergeBadges(sqlite.dumpBadges());
            }

            PlayerProfile alex = db.loadPlayer(ALEX);
            assertThat(alex.getPlaytimeSeconds()).isEqualTo(3600);
            assertThat(alex.getPinnedBadges()).containsExactly("veteran");
            assertThat(alex.isCompleted("miner")).isTrue();
            assertThat(db.dumpAchievementProgress()).containsExactly(new AchievementProgressRow(ALEX, "miner", 100, 4000));
            assertThat(db.dumpBadges()).containsExactly(new BadgeRow(ALEX, "veteran", 777, "Admin"));
            assertThat(db.countPlayers("dpp_")).isEqualTo(1);
        } finally {
            sqlite.shutdown();
        }
    }
}
