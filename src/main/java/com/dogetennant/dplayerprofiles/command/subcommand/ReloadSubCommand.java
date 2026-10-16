package com.dogetennant.dplayerprofiles.command.subcommand;

import com.dogetennant.dplayerprofiles.DPlayerProfiles;
import com.dogetennant.dplayerprofiles.command.SubCommand;
import com.dogetennant.dplayerprofiles.lang.MessageKey;
import com.dogetennant.dplayerprofiles.lang.Placeholder;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class ReloadSubCommand implements SubCommand {

    @Override
    public String getName() { return "reload"; }

    @Override
    public String getPermission() { return "dplayerprofiles.admin"; }

    @Override
    public MessageKey getDescriptionKey() { return MessageKey.CMD_DESC_RELOAD; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        DPlayerProfiles plugin = DPlayerProfiles.getInstance();
        // a file with a YAML error keeps what was loaded from it before
        if (!plugin.getConfigManager().load()) brokenFile(sender, "config.yml");
        plugin.getLangManager().load(plugin.getConfigManager().get().language);
        if (!plugin.getAchievementRewardStorage().load()) brokenFile(sender, "rewards.yml");
        plugin.getAchievementConfigLoader().load();
        plugin.getBadgeConfigLoader().load();
        plugin.getGuiLayoutLoader().load();
        plugin.getPlacedBlockTracker().applyConfig();
        plugin.getWebStatsExporter().applyConfig();
        // the playtime task runs at the interval it was started with
        plugin.getPlaytimeTracker().start(plugin.getConfigManager().get().playtimeUpdateInterval);
        if (sender instanceof Player p) {
            plugin.getLangManager().send(p, MessageKey.RELOAD_SUCCESS);
        } else {
            sender.sendMessage("Configuration reloaded.");
        }
    }

    private static void brokenFile(CommandSender sender, String file) {
        DPlayerProfiles plugin = DPlayerProfiles.getInstance();
        if (sender instanceof Player p) {
            plugin.getLangManager().send(p, MessageKey.RELOAD_FILE_BROKEN, Placeholder.of("file", file));
        } else {
            sender.sendMessage(file + " has a mistake (YAML error) - what it held before stays in use.");
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) { return List.of(); }
}
