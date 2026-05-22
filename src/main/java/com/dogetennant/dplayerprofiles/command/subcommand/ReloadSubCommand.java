package com.dogetennant.dplayerprofiles.command.subcommand;

import com.dogetennant.dplayerprofiles.DPlayerProfiles;
import com.dogetennant.dplayerprofiles.command.SubCommand;
import com.dogetennant.dplayerprofiles.lang.MessageKey;
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
        plugin.getConfigManager().load();
        plugin.getLangManager().load(plugin.getConfigManager().get().language);
        plugin.getAchievementRewardStorage().load();
        plugin.getAchievementConfigLoader().load();
        plugin.getBadgeConfigLoader().load();
        plugin.getGuiLayoutLoader().load();
        if (sender instanceof Player p) {
            plugin.getLangManager().send(p, MessageKey.RELOAD_SUCCESS);
        } else {
            sender.sendMessage("Configuration reloaded.");
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) { return List.of(); }
}
