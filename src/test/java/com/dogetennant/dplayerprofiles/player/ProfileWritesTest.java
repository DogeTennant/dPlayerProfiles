package com.dogetennant.dplayerprofiles.player;

import com.dogetennant.dplayerprofiles.DPlayerProfiles;
import com.dogetennant.dplayerprofiles.Fakes;
import com.dogetennant.dplayerprofiles.database.SQLiteManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Profile writes reach the database in the order they were made, whatever order the background
 * threads would run them in.
 */
class ProfileWritesTest {

    private static final UUID ALEX = UUID.fromString("00000000-0000-0000-0000-00000000a1e7");

    @TempDir
    Path dataFolder;

    private final DPlayerProfiles plugin = Fakes.plugin(Fakes.config());
    private SQLiteManager db;
    private ProfileManager profiles;

    @BeforeEach
    void setUp() throws Exception {
        db = new SQLiteManager(dataFolder.toFile(), "");
        db.initialize();
        db.upsertPlayer(ALEX, "Alex", 0, 0, 0, 1, "2026-10-09");
        profiles = new ProfileManager(plugin, db);
    }

    @AfterEach
    void tearDown() {
        db.shutdown();
    }

    @Test
    void theCompletionIsNotOverwrittenByTheProgressWrittenJustBeforeIt() throws Exception {
        profiles.saveProgress(ALEX, "miner", 999, 0);
        profiles.saveProgress(ALEX, "miner", 1000, 5555);

        Fakes.runBackground(true);      // two pool threads: the later write may run first

        assertThat(db.loadPlayer(ALEX).isCompleted("miner")).isTrue();
        assertThat(db.loadPlayer(ALEX).getProgress("miner")).isEqualTo(1000);
    }

    @Test
    void theLastProgressIsWhatIsStored() throws Exception {
        profiles.saveProgress(ALEX, "time_played", 60, 0);
        profiles.saveProgress(ALEX, "time_played", 120, 0);

        Fakes.runBackground(true);

        assertThat(db.loadPlayer(ALEX).getProgress("time_played")).isEqualTo(120);
    }
}
