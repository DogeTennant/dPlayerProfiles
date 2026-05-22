package com.dogetennant.dplayerprofiles;

import com.dogetennant.dplayerprofiles.achievement.AchievementListener;
import com.dogetennant.dplayerprofiles.achievement.AchievementManager;
import com.dogetennant.dplayerprofiles.command.AchievementsCommand;
import com.dogetennant.dplayerprofiles.command.CommandRegistry;
import com.dogetennant.dplayerprofiles.command.DPlayerProfilesCommand;
import com.dogetennant.dplayerprofiles.command.ProfileCommand;
import com.dogetennant.dplayerprofiles.command.subcommand.*;
import com.dogetennant.dplayerprofiles.config.AchievementConfigLoader;
import com.dogetennant.dplayerprofiles.config.BadgeConfigLoader;
import com.dogetennant.dplayerprofiles.config.ConfigManager;
import com.dogetennant.dplayerprofiles.config.GuiLayoutLoader;
import com.dogetennant.dplayerprofiles.database.DatabaseManager;
import com.dogetennant.dplayerprofiles.database.MySQLManager;
import com.dogetennant.dplayerprofiles.database.SQLiteManager;
import com.dogetennant.dplayerprofiles.gui.GuiManager;
import com.dogetennant.dplayerprofiles.integration.IntegrationManager;
import com.dogetennant.dplayerprofiles.lang.LanguageManager;
import com.dogetennant.dplayerprofiles.player.ChatBadgeListener;
import com.dogetennant.dplayerprofiles.player.PlaytimeTracker;
import com.dogetennant.dplayerprofiles.player.ProfileManager;
import com.dogetennant.dplayerprofiles.reward.AchievementRewardStorage;
import com.dogetennant.dplayerprofiles.reward.RewardManager;
import com.dogetennant.dplayerprofiles.util.LogUtil;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;

public class DPlayerProfiles extends JavaPlugin {

    private static DPlayerProfiles instance;

    private ConfigManager configManager;
    private LanguageManager langManager;
    private DatabaseManager databaseManager;
    private AchievementRewardStorage achievementRewardStorage;
    private AchievementConfigLoader achievementConfigLoader;
    private BadgeConfigLoader badgeConfigLoader;
    private ProfileManager profileManager;
    private PlaytimeTracker playtimeTracker;
    private RewardManager rewardManager;
    private AchievementManager achievementManager;
    private GuiManager guiManager;
    private GuiLayoutLoader guiLayoutLoader;
    private IntegrationManager integrationManager;
    private CommandRegistry commandRegistry;
    private DPlayerProfilesAPI api;

    @Override
    public void onEnable() {
        instance = this;
        LogUtil.init(this);

        // 1. Config
        configManager = new ConfigManager(this);
        configManager.load();

        // 2. Language
        langManager = new LanguageManager(this);
        langManager.load(configManager.get().language);

        // 3. Database
        try {
            databaseManager = createDatabase();
            databaseManager.initialize();
        } catch (SQLException e) {
            LogUtil.severe("Database initialisation failed - disabling plugin.", e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // 4. Reward storage (before achievement config loader so it can overlay rewards)
        achievementRewardStorage = new AchievementRewardStorage(this);
        achievementRewardStorage.load();

        // 5. Config loaders
        achievementConfigLoader = new AchievementConfigLoader(this);
        achievementConfigLoader.setRewardStorage(achievementRewardStorage);
        achievementConfigLoader.load();

        badgeConfigLoader = new BadgeConfigLoader(this);
        badgeConfigLoader.load();

        // 6. Reward manager
        rewardManager = new RewardManager(langManager);
        // Wire badge granting back so RewardManager can grant badges without circular dep
        rewardManager.setBadgeGranter((player, badgeId) -> {
            var profile = profileManager != null ? profileManager.get(player.getUniqueId()) : null;
            if (profile != null && !profile.hasBadge(badgeId)
                    && badgeConfigLoader.get(badgeId) != null) {
                long now = System.currentTimeMillis();
                profile.grantBadge(badgeId, now);
                profileManager.saveBadge(player.getUniqueId(), badgeId, now, null);
                achievementManager.checkChainAchievements(player, profile);
            }
        });

        // 7. Achievement manager
        achievementManager = new AchievementManager(this, achievementConfigLoader, rewardManager);

        // 8. Profile manager (handles join/quit events)
        profileManager = new ProfileManager(this, databaseManager);
        getServer().getPluginManager().registerEvents(profileManager, this);

        // 9. Playtime tracker (also handles AFK detection events)
        playtimeTracker = new PlaytimeTracker(this);
        getServer().getPluginManager().registerEvents(playtimeTracker, this);
        playtimeTracker.start(configManager.get().playtimeUpdateInterval);

        // 10. Achievement listener (Bukkit events)
        getServer().getPluginManager().registerEvents(new AchievementListener(this), this);
        getServer().getPluginManager().registerEvents(new ChatBadgeListener(this), this);

        // 11. GUI manager + layout loader
        guiManager = new GuiManager(this);
        getServer().getPluginManager().registerEvents(guiManager, this);

        guiLayoutLoader = new GuiLayoutLoader(this);
        guiLayoutLoader.load();

        // 12. Integrations
        integrationManager = new IntegrationManager(this);
        integrationManager.init();

        // 13. Commands
        commandRegistry = new CommandRegistry();
        commandRegistry.register(new HelpSubCommand());
        commandRegistry.register(new MeSubCommand());
        commandRegistry.register(new ProfileSubCommand());
        commandRegistry.register(new AchievementsSubCommand());
        commandRegistry.register(new LeaderboardSubCommand());
        commandRegistry.register(new RewardsSubCommand());
        commandRegistry.register(new LanguageSubCommand());
        commandRegistry.register(new ReloadSubCommand());
        commandRegistry.register(new ResetSubCommand());
        commandRegistry.register(new ResetAchievementSubCommand());
        commandRegistry.register(new CompleteSubCommand());
        commandRegistry.register(new BadgeSubCommand());
        commandRegistry.register(new PrivacySubCommand());

        DPlayerProfilesCommand commandHandler = new DPlayerProfilesCommand(commandRegistry);
        var cmd = getCommand("dplayerprofiles");
        if (cmd != null) {
            cmd.setExecutor(commandHandler);
            cmd.setTabCompleter(commandHandler);
        }

        // Standalone /profile and /me
        ProfileCommand profileCommand = new ProfileCommand();
        var profileCmd = getCommand("profile");
        if (profileCmd != null) {
            profileCmd.setExecutor(profileCommand);
            profileCmd.setTabCompleter(profileCommand);
        }

        // Standalone /achievements
        AchievementsCommand achievementsCommand = new AchievementsCommand();
        var achievementsCmd = getCommand("achievements");
        if (achievementsCmd != null) {
            achievementsCmd.setExecutor(achievementsCommand);
            achievementsCmd.setTabCompleter(achievementsCommand);
        }

        // 14. Public API
        api = new DPlayerProfilesAPI(profileManager, achievementManager,
                achievementConfigLoader, badgeConfigLoader);

        LogUtil.info("dPlayerProfiles v" + getDescription().getVersion() + " enabled.");
    }

    @Override
    public void onDisable() {
        if (playtimeTracker != null) playtimeTracker.shutdown();
        if (integrationManager != null) integrationManager.shutdown();
        if (databaseManager != null) databaseManager.shutdown();
        LogUtil.info("dPlayerProfiles disabled.");
    }

    private DatabaseManager createDatabase() {
        if (configManager.get().storageType.equals("mysql")) {
            return new MySQLManager(configManager.get());
        }
        return new SQLiteManager(getDataFolder(), configManager.get().tablePrefix);
    }

    public static DPlayerProfiles getInstance() { return instance; }
    public ConfigManager getConfigManager() { return configManager; }
    public LanguageManager getLangManager() { return langManager; }
    public DatabaseManager getDatabaseManager() { return databaseManager; }
    public AchievementRewardStorage getAchievementRewardStorage() { return achievementRewardStorage; }
    public AchievementConfigLoader getAchievementConfigLoader() { return achievementConfigLoader; }
    public BadgeConfigLoader getBadgeConfigLoader() { return badgeConfigLoader; }
    public ProfileManager getProfileManager() { return profileManager; }
    public AchievementManager getAchievementManager() { return achievementManager; }
    public GuiManager getGuiManager() { return guiManager; }
    public GuiLayoutLoader getGuiLayoutLoader() { return guiLayoutLoader; }
    public IntegrationManager getIntegrationManager() { return integrationManager; }
    public RewardManager getRewardManager() { return rewardManager; }
    public CommandRegistry getCommandRegistry() { return commandRegistry; }
    public DPlayerProfilesAPI getAPI() { return api; }
}
