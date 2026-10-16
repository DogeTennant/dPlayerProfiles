package com.dogetennant.dplayerprofiles.server;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import static org.assertj.core.api.Assertions.assertThat;

/** Admin commands on online players, private profiles, chat prompts and points nodes. */
class PlayersServerTest {

    private TestServer mc;

    @BeforeEach
    void start() {
        mc = TestServer.start();
        mc.achievement("miner", "BLOCK_BREAK", "STONE", 3, 10);
        mc.achievement("veteran", "MANUAL", "*", 1, 100);
        mc.console("dp reload");
    }

    @AfterEach
    void stop() {
        mc.stop();
    }

    private void breakStone(PlayerMock player, int x) {
        Block stone = mc.world.getBlockAt(x, 10, 0);
        stone.setType(Material.STONE);
        player.simulateBlockBreak(stone);
        mc.settle();
    }

    // Record: docs/problems/dplayerprofiles-reset-online-player-loses-profile.md
    @Test
    void anOnlinePlayerResetStartsAgainAtOnceAndWhatTheyDoNextCountsAndIsKept() {
        PlayerMock alex = mc.join("Alex");
        breakStone(alex, 0);
        assertThat(mc.profile(alex).getProgress("miner")).isEqualTo(1);

        mc.console("dp reset Alex");
        assertThat(mc.profile(alex)).isNotNull();
        assertThat(mc.profile(alex).getProgress("miner")).isZero();
        breakStone(alex, 1);
        breakStone(alex, 2);
        breakStone(alex, 3);
        assertThat(mc.profile(alex).isCompleted("miner")).isTrue();

        alex.disconnect();
        mc.settle();
        alex.reconnect();
        mc.settle();
        assertThat(mc.profile(alex).isCompleted("miner")).isTrue();
    }

    // Record: docs/problems/dplayerprofiles-achievements-of-private-profiles-shown.md
    @Test
    void aPrivateProfileAndItsAchievementsAreRefusedToOthersButNotToItsOwner() {
        PlayerMock bob = mc.join("Bob");
        mc.command(bob, "dp private on");
        PlayerMock alex = mc.join("Alex");

        for (String command : new String[] { "profile Bob", "dp profile Bob", "achievements Bob", "dp achievements Bob" }) {
            TestServer.messages(alex);
            mc.command(alex, command);
            assertThat(TestServer.messages(alex)).as(command).anyMatch(m -> m.contains("Bob's profile is private"));
            assertThat(alex.getOpenInventory().getTopInventory()).as(command).isNull();
        }

        mc.command(bob, "achievements Bob");
        assertThat(bob.getOpenInventory().getTopInventory()).isNotNull();
    }

    // Record: docs/problems/dplayerprofiles-chat-prompt-survives-quit.md
    @Test
    void aRewardPromptEndsWhenTheAdminQuits() {
        PlayerMock admin = mc.admin("Admin");
        mc.command(admin, "dp rewards");
        mc.click(admin, 9);                      // the first achievement
        mc.click(admin, 36);                     // "add a command": the prompt waits for chat
        admin.disconnect();
        mc.settle();
        admin.reconnect();
        mc.settle();

        admin.chat("hello everyone");
        mc.server.getScheduler().waitAsyncEventsFinished();
        mc.settle();

        assertThat(mc.file("rewards.yml")).doesNotExist();
    }

    // Record: docs/problems/dplayerprofiles-playtime-interval-changed-by-reload.md
    @Test
    void aChangedPlaytimeIntervalReadByReloadStillCountsAMinuteAsAMinute() {
        mc.write("config.yml", mc.read("config.yml")
                .replace("playtime-update-interval: 60", "playtime-update-interval: 300")
                .replace("  enabled: true\n  timeout: 300", "  enabled: false\n  timeout: 300"));   // anti-afk
        mc.console("dp reload");
        PlayerMock alex = mc.join("Alex");

        mc.server.getScheduler().performTicks(60 * 20);    // one minute
        mc.settle();

        assertThat(mc.profile(alex).getPlaytimeSeconds()).isZero();       // the first run is after 5 minutes
        mc.server.getScheduler().performTicks(4 * 60 * 20);
        mc.settle();
        assertThat(mc.profile(alex).getPlaytimeSeconds()).isEqualTo(300);
    }

    @Test
    void aPointsNodeRewardIsPaidWhenTheNodeIsReached() {
        mc.write("rewards.yml", """
                points_node_0:
                  commands:
                    - record {player} node 1
                """);
        mc.console("dp reload");
        PlayerMock alex = mc.join("Alex");

        mc.console("dp complete Alex veteran");      // 100 points = the first node

        assertThat(mc.recorded).containsExactly("Alex node 1");
    }
}
