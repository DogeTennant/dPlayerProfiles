package com.dogetennant.dplayerprofiles.server;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.EntityType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.LivingEntityMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import static org.assertj.core.api.Assertions.assertThat;

/** A player's profile and achievements from joining to rejoining. */
class ProfileServerTest {

    private TestServer mc;

    @BeforeEach
    void start() {
        mc = TestServer.start();
    }

    @AfterEach
    void stop() {
        mc.stop();
    }

    @Test
    void startsCleanWithItsShippedFiles() {
        assertThat(mc.plugin.isEnabled()).isTrue();
        assertThat(mc.warnings).isEmpty();
    }

    @Test
    void achievementsCountAndTheirRewardsArePaidAndKeptAcrossARejoin() {
        mc.achievement("miner", "BLOCK_BREAK", "STONE", 3, 10);
        mc.achievement("zombies", "MOB_KILL", "ZOMBIE", 1, 5);
        mc.console("dp reload");
        PlayerMock alex = mc.join("Alex");
        assertThat(mc.profile(alex)).isNotNull();

        for (int x = 0; x < 3; x++) {
            Block stone = mc.world.getBlockAt(x, 10, 0);
            stone.setType(Material.STONE);
            alex.simulateBlockBreak(stone);
        }
        LivingEntity zombie = (LivingEntity) mc.world.spawnEntity(alex.getLocation(), EntityType.ZOMBIE);
        ((LivingEntityMock) zombie).setKiller(alex);   // Paper records who dealt the killing blow
        zombie.damage(1000, alex);
        mc.settle();

        assertThat(mc.profile(alex).isCompleted("miner")).isTrue();
        assertThat(mc.profile(alex).isCompleted("zombies")).isTrue();

        alex.disconnect();
        mc.settle();
        alex.reconnect();
        mc.settle();

        assertThat(mc.profile(alex).isCompleted("miner")).isTrue();
        assertThat(mc.profile(alex).isCompleted("zombies")).isTrue();
        assertThat(mc.plugin.getAchievementManager().calculateTotalPoints(mc.profile(alex))).isEqualTo(15);
    }
}
