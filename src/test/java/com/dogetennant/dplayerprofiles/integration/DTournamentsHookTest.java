package com.dogetennant.dplayerprofiles.integration;

import com.dogetennant.dplayerprofiles.DPlayerProfiles;
import com.dogetennant.dplayerprofiles.Fakes;
import com.dogetennant.dplayerprofiles.achievement.AchievementManager;
import com.dogetennant.dplayerprofiles.config.AchievementConfigLoader;
import com.dogetennant.dplayerprofiles.model.TriggerType;
import com.dogetennant.dplayerprofiles.player.ProfileManager;
import com.dogetennant.dtournaments.api.events.TournamentEndEvent;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** A dTournaments win counts for TOURNAMENT_WIN achievements, also when the winner is offline. */
class DTournamentsHookTest {

    private static final UUID ALEX = UUID.fromString("00000000-0000-0000-0000-00000000a1e7");

    private final DPlayerProfiles plugin = Fakes.plugin(Fakes.config());
    private final AchievementManager achievements = mock(AchievementManager.class);
    private final ProfileManager profiles = mock(ProfileManager.class);
    private final AchievementConfigLoader loader = mock(AchievementConfigLoader.class);
    private final TournamentEndEvent end = mock(TournamentEndEvent.class);
    private DTournamentsHook hook;

    @BeforeEach
    void setUp() {
        when(plugin.getAchievementManager()).thenReturn(achievements);
        when(plugin.getProfileManager()).thenReturn(profiles);
        when(plugin.getAchievementConfigLoader()).thenReturn(loader);
        when(loader.isWatched(TriggerType.TOURNAMENT_WIN, "weekly")).thenReturn(true);
        when(end.getWinner()).thenReturn(ALEX);
        when(end.getConfigId()).thenReturn("weekly");
        when(Fakes.server().getPlayer(ALEX)).thenReturn(null);
        hook = new DTournamentsHook(plugin);
    }

    @Test
    void anOnlineWinnerGetsTheWinAtOnce() {
        Player alex = mock(Player.class);
        when(alex.getUniqueId()).thenReturn(ALEX);
        when(Fakes.server().getPlayer(ALEX)).thenReturn(alex);
        when(profiles.isLoaded(ALEX)).thenReturn(true);

        hook.onTournamentEnd(end);

        verify(achievements).increment(alex, TriggerType.TOURNAMENT_WIN, "weekly", 1);
        verify(profiles, never()).addPendingTrigger(any(), any(), any(), anyLong());
    }

    @Test
    void anOfflineWinnersWinWaitsForTheirNextLogin() {
        hook.onTournamentEnd(end);

        verify(profiles).addPendingTrigger(ALEX, TriggerType.TOURNAMENT_WIN, "weekly", 1);
        verify(achievements, never()).increment(any(), any(), any(), anyLong());
    }

    @Test
    void aTournamentNoAchievementWatchesIsIgnored() {
        when(end.getConfigId()).thenReturn("daily");

        hook.onTournamentEnd(end);

        verify(profiles, never()).addPendingTrigger(any(), any(), any(), anyLong());
    }
}
