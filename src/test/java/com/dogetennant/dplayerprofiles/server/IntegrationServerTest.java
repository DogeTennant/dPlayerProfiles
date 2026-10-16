package com.dogetennant.dplayerprofiles.server;

import com.DogeTennant.oneinthechamberreborn.api.events.OITCGameWinEvent;
import com.DogeTennant.oneinthechamberreborn.api.events.OITCKillEvent;
import com.DogeTennant.oneinthechamberreborn.arena.Arena;
import com.dogetennant.dtournaments.api.events.TournamentEndEvent;
import com.dogetennant.dtournaments.tournament.TournamentResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Achievements from our other plugins' events, built with their real classes (the contract). */
class IntegrationServerTest {

    private TestServer mc;

    @BeforeEach
    void start() {
        mc = TestServer.start();
        mc.achievement("champion", "TOURNAMENT_WIN", "weekly_kills", 1, 0);
        mc.achievement("sharpshooter", "OITC_KILL", "*", 2, 0);
        mc.achievement("oitc_winner", "OITC_WIN", "castle", 1, 0);
        mc.console("dp reload");
    }

    @AfterEach
    void stop() {
        mc.stop();
    }

    private void tournamentWon(UUID winner) {
        mc.fire(new TournamentEndEvent(new TournamentResult(UUID.randomUUID(), "weekly_kills",
                System.currentTimeMillis(), List.of(Map.entry(winner, 120L)))));
        mc.settle();
    }

    @Test
    void aTournamentWonOnlineCountsAtOnceAndOneWonOfflineAtTheNextLogin() {
        PlayerMock alex = mc.join("Alex");
        tournamentWon(alex.getUniqueId());
        assertThat(mc.profile(alex).isCompleted("champion")).isTrue();

        PlayerMock bob = mc.join("Bob");
        bob.disconnect();
        mc.settle();
        tournamentWon(bob.getUniqueId());
        bob.reconnect();
        mc.settle();
        assertThat(mc.profile(bob).isCompleted("champion")).isTrue();
    }

    @Test
    void oneInTheChamberKillsAndWinsCount() {
        PlayerMock alex = mc.join("Alex");
        PlayerMock bob = mc.join("Bob");
        Arena castle = new Arena("castle");

        mc.fire(new OITCKillEvent(alex, bob, castle, 1, true));
        mc.fire(new OITCKillEvent(alex, bob, castle, 2, false));
        mc.fire(new OITCGameWinEvent(alex, castle, 2, 0, 1));
        mc.settle();

        assertThat(mc.profile(alex).isCompleted("sharpshooter")).isTrue();
        assertThat(mc.profile(alex).isCompleted("oitc_winner")).isTrue();
    }
}
