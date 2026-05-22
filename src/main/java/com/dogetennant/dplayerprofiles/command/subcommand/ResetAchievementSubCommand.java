package com.dogetennant.dplayerprofiles.command.subcommand;

import com.dogetennant.dplayerprofiles.DPlayerProfiles;
import com.dogetennant.dplayerprofiles.command.SubCommand;
import com.dogetennant.dplayerprofiles.lang.MessageKey;
import com.dogetennant.dplayerprofiles.lang.Placeholder;
import com.dogetennant.dplayerprofiles.util.LogUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

public class ResetAchievementSubCommand implements SubCommand {

    @Override
    public String getName() { return "resetach"; }

    @Override
    public String getPermission() { return "dplayerprofiles.admin"; }

    @Override
    public MessageKey getDescriptionKey() { return MessageKey.CMD_DESC_RESETACH; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        DPlayerProfiles plugin = DPlayerProfiles.getInstance();
        if (args.length < 3) {
            if (sender instanceof Player p) plugin.getLangManager().send(p, MessageKey.CMD_RESETACH_USAGE);
            else sender.sendMessage("Usage: /dp resetach <player> <achievement>");
            return;
        }

        String playerName = args[1];
        String achievementId = args[2];

        if (plugin.getAchievementConfigLoader().get(achievementId) == null) {
            if (sender instanceof Player p) {
                plugin.getLangManager().send(p, MessageKey.CMD_RESETACH_NOT_FOUND,
                        Placeholder.of("achievement", achievementId));
            } else {
                sender.sendMessage("Achievement not found: " + achievementId);
            }
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayerIfCached(playerName);
        if (target == null) target = Bukkit.getOfflinePlayer(playerName);
        OfflinePlayer finalTarget = target;

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                plugin.getProfileManager().resetAchievement(finalTarget.getUniqueId(), achievementId);
                if (sender instanceof Player p) {
                    plugin.getServer().getScheduler().runTask(plugin, () ->
                            plugin.getLangManager().send(p, MessageKey.CMD_RESETACH_SUCCESS,
                                    Placeholder.of("player", playerName),
                                    Placeholder.of("achievement", achievementId)));
                } else {
                    sender.sendMessage("Reset achievement " + achievementId + " for " + playerName);
                }
            } catch (SQLException e) {
                LogUtil.severe("Failed to reset achievement for " + playerName, e);
            }
        });
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        DPlayerProfiles plugin = DPlayerProfiles.getInstance();
        if (args.length == 2) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 3) {
            return plugin.getAchievementConfigLoader().getAll().keySet().stream()
                    .filter(id -> id.startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return List.of();
    }
}
