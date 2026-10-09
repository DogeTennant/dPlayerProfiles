package com.dogetennant.dplayerprofiles.player;

import com.dogetennant.dplayerprofiles.DPlayerProfiles;
import com.dogetennant.dplayerprofiles.Fakes;
import com.dogetennant.dplayerprofiles.achievement.AchievementManager;
import com.dogetennant.dplayerprofiles.database.SQLiteManager;
import com.dogetennant.dplayerprofiles.model.TriggerType;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Logging in and out: the profile, what waited while the player was offline, the shutdown save. */
class ProfileLoginTest {

    private static final UUID ALEX = UUID.fromString("00000000-0000-0000-0000-00000000a1e7");

    @TempDir
    Path dataFolder;

    private final DPlayerProfiles plugin = Fakes.plugin(Fakes.config());
    private final AchievementManager achievements = mock(AchievementManager.class);
    private final Player alex = mock(Player.class);
    private SQLiteManager db;
    private ProfileManager profiles;

    @BeforeEach
    void setUp() throws Exception {
        db = new SQLiteManager(dataFolder.toFile(), "");
        db.initialize();
        when(plugin.getAchievementManager()).thenReturn(achievements);
        when(alex.getUniqueId()).thenReturn(ALEX);
        when(alex.getName()).thenReturn("Alex");
        when(alex.isOnline()).thenReturn(true);
        profiles = new ProfileManager(plugin, db);
    }

    @AfterEach
    void tearDown() {
        db.shutdown();
    }

    private void join() {
        PlayerJoinEvent join = mock(PlayerJoinEvent.class);
        when(join.getPlayer()).thenReturn(alex);
        profiles.onLogin(join);
    }

    private void quit() {
        PlayerQuitEvent quit = mock(PlayerQuitEvent.class);
        when(quit.getPlayer()).thenReturn(alex);
        profiles.onQuit(quit);
    }

    @Test
    void aNewPlayerGetsAProfileAndTheLoginCounts() throws Exception {
        join();

        assertThat(profiles.get(ALEX)).isNotNull();
        assertThat(db.loadPlayer(ALEX)).isNotNull();
        verify(achievements).onLogin(any(), any());
    }

    @Test
    void aTournamentWonWhileOfflineCountsAtTheNextLogin() throws Exception {
        db.upsertPlayer(ALEX, "Alex", 0, 0, 0, 1, "2026-10-01");
        profiles.addPendingTrigger(ALEX, TriggerType.TOURNAMENT_WIN, "weekly", 1);

        join();

        verify(achievements).increment(alex, TriggerType.TOURNAMENT_WIN, "weekly", 1);
        assertThat(db.getPendingTriggers(ALEX)).isEmpty();
    }

    @Test
    void aPlayerWhoLeftBeforeTheProfileLoadedIsNotKeptAndTheirTriggersWait() throws Exception {
        db.upsertPlayer(ALEX, "Alex", 0, 0, 0, 1, "2026-10-01");
        profiles.addPendingTrigger(ALEX, TriggerType.TOURNAMENT_WIN, "weekly", 1);
        when(alex.isOnline()).thenReturn(false);

        join();

        assertThat(profiles.get(ALEX)).isNull();
        verify(achievements, never()).increment(any(), any(), any(), anyLong());
        assertThat(db.getPendingTriggers(ALEX)).hasSize(1);
    }

    @Test
    void quittingSavesThePlaytime() throws Exception {
        join();
        profiles.get(ALEX).addPlaytime(90);

        quit();

        assertThat(db.loadPlayer(ALEX).getPlaytimeSeconds()).isEqualTo(90);
        assertThat(profiles.get(ALEX)).isNull();
    }

    @Test
    void stoppingTheServerSavesEveryOnlinePlayer() throws Exception {
        join();
        profiles.get(ALEX).addPlaytime(45);
        doReturn(List.of(alex)).when(Fakes.server()).getOnlinePlayers();

        profiles.saveAll();

        assertThat(db.loadPlayer(ALEX).getPlaytimeSeconds()).isEqualTo(45);
    }
}
