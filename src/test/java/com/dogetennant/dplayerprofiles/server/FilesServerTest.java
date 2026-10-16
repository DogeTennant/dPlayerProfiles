package com.dogetennant.dplayerprofiles.server;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * rewards.yml (written by the rewards editor) and config.yml: edits by hand and typos. A file with a
 * YAML error is never saved over and keeps what was loaded from it before.
 * Records: docs/problems/dplayerprofiles-rewards-file-saved-over.md,
 * docs/problems/dplayerprofiles-language-switch-saves-whole-config.md,
 * docs/problems/dplayerprofiles-config-typo-runs-on-defaults.md
 */
class FilesServerTest {

    /** The editor's first achievement (slot 9 of /dp rewards), and its "clear rewards" button. */
    private static final int FIRST_ACHIEVEMENT = 9;
    private static final int CLEAR = 44;

    /** Rewards of the miner achievement and of the first points node. */
    private static final String REWARDS = """
            miner:
              commands:
                - give {player} diamond 1
            points_node_0:
              commands:
                - give {player} emerald 5
            """;

    /** A line YAML cannot read. */
    private static final String TYPO = "broken: [\n";

    private TestServer mc;

    @BeforeEach
    void start() {
        mc = TestServer.start();
        mc.achievement("miner", "BLOCK_BREAK", "STONE", 3, 10);
    }

    @AfterEach
    void stop() {
        mc.stop();
    }

    /** The admin clears the miner's rewards in /dp rewards. */
    private void clearMinerRewards(PlayerMock admin) {
        mc.command(admin, "dp rewards");
        mc.click(admin, FIRST_ACHIEVEMENT);
        mc.click(admin, CLEAR);
        mc.settle();
    }

    @Test
    void aTypoInRewardsYmlKeepsTheRewardsFromBeforeAndTheEditorRefusesChanges() {
        PlayerMock admin = mc.admin("Admin");
        mc.write("rewards.yml", REWARDS);
        mc.command(admin, "dp reload");
        assertThat(mc.plugin.getAchievementConfigLoader().get("miner").rewards).hasSize(1);

        mc.write("rewards.yml", REWARDS + TYPO);
        TestServer.messages(admin);
        mc.command(admin, "dp reload");
        assertThat(TestServer.messages(admin)).anyMatch(m -> m.contains("rewards.yml has a mistake"));
        assertThat(mc.plugin.getAchievementConfigLoader().get("miner").rewards).hasSize(1);

        clearMinerRewards(admin);

        assertThat(TestServer.messages(admin)).anyMatch(m -> m.contains("rewards.yml has a mistake"));
        assertThat(mc.read("rewards.yml")).isEqualTo(REWARDS + TYPO);
        assertThat(mc.plugin.getAchievementConfigLoader().get("miner").rewards).hasSize(1);
    }

    @Test
    void rewardsYmlEditedByHandKeepsTheEditWhenTheEditorChangesAnotherReward() {
        mc.console("dp reload");
        PlayerMock admin = mc.admin("Admin");
        mc.write("rewards.yml", REWARDS);                // by hand, while the server runs

        clearMinerRewards(admin);

        assertThat(mc.read("rewards.yml")).contains("points_node_0", "emerald").doesNotContain("diamond");
    }

    @Test
    void switchingLanguageChangesOnlyThatLineAndKeepsAHandEditAndTheComments() {
        String edited = mc.read("config.yml").replace("leaderboard-size: 10", "leaderboard-size: 25");
        mc.write("config.yml", edited);

        mc.console("dp language cs_cz");

        assertThat(mc.read("config.yml")).contains("language: cs_cz", "leaderboard-size: 25", "# sqlite or mysql");
    }

    @Test
    void switchingLanguageAfterATypoInConfigYmlLeavesTheFileAlone() {
        String broken = """
                storage:
                  type: mysql
                  table-prefix: dpp_
                """ + TYPO;
        mc.write("config.yml", broken);
        mc.console("dp reload");

        mc.console("dp language cs_cz");

        assertThat(mc.read("config.yml")).isEqualTo(broken);
        assertThat(mc.plugin.getLangManager().getCurrentLanguage()).isEqualTo("cs_cz");
    }

    @Test
    void aTypoInConfigYmlKeepsThePluginOffInsteadOfRunningOnTheBuiltInSettings() {
        mc.write("config.yml", """
                storage:
                  type: mysql
                  table-prefix: dpp_
                """ + TYPO);
        mc.restart();

        assertThat(mc.plugin.isEnabled()).isFalse();
        assertThat(mc.warnings).anyMatch(w -> w.contains("config.yml has a mistake"));
    }

    @Test
    void aReloadWithATypoInConfigYmlKeepsTheSettingsFromBefore() {
        PlayerMock admin = mc.admin("Admin");
        mc.write("config.yml", mc.read("config.yml").replace("leaderboard-size: 10", "leaderboard-size: 25"));
        mc.command(admin, "dp reload");
        assertThat(mc.plugin.getConfigManager().get().leaderboardSize).isEqualTo(25);

        mc.write("config.yml", "storage:\n  type: mysql\n" + TYPO);
        TestServer.messages(admin);
        mc.command(admin, "dp reload");

        assertThat(TestServer.messages(admin)).anyMatch(m -> m.contains("config.yml has a mistake"));
        assertThat(mc.plugin.getConfigManager().get().leaderboardSize).isEqualTo(25);
        assertThat(mc.plugin.getConfigManager().get().storageType).isEqualTo("sqlite");
    }
}
