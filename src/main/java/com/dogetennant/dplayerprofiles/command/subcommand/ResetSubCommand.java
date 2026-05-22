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

public class ResetSubCommand implements SubCommand {

    @Override
    public String getName() { return "reset"; }

    @Override
    public String getPermission() { return "dplayerprofiles.admin"; }

    @Override
    public MessageKey getDescriptionKey() { return MessageKey.CMD_DESC_RESET; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        DPlayerProfiles plugin = DPlayerProfiles.getInstance();
        if (args.length < 2) {
            if (sender instanceof Player p) plugin.getLangManager().send(p, MessageKey.CMD_RESET_USAGE);
            else sender.sendMessage("Usage: /dp reset <player>");
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayerIfCached(args[1]);
        if (target == null) {
            target = Bukkit.getOfflinePlayer(args[1]);
        }
        String name = args[1];
        OfflinePlayer finalTarget = target;

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                plugin.getProfileManager().resetPlayer(finalTarget.getUniqueId());
                if (sender instanceof Player p) {
                    plugin.getServer().getScheduler().runTask(plugin, () ->
                            plugin.getLangManager().send(p, MessageKey.CMD_RESET_SUCCESS,
                                    Placeholder.of("player", name)));
                } else {
                    sender.sendMessage("Reset data for " + name);
                }
            } catch (SQLException e) {
                LogUtil.severe("Failed to reset player " + name, e);
            }
        });
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return List.of();
    }
}
