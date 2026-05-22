package com.dogetennant.dplayerprofiles.command.subcommand;

import com.dogetennant.dplayerprofiles.DPlayerProfiles;
import com.dogetennant.dplayerprofiles.command.SubCommand;
import com.dogetennant.dplayerprofiles.lang.MessageKey;
import com.dogetennant.dplayerprofiles.lang.Placeholder;
import com.dogetennant.dplayerprofiles.model.AchievementConfig;
import com.dogetennant.dplayerprofiles.model.PlayerProfile;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Collectors;

public class CompleteSubCommand implements SubCommand {

    @Override
    public String getName() { return "complete"; }

    @Override
    public String getPermission() { return "dplayerprofiles.admin"; }

    @Override
    public MessageKey getDescriptionKey() { return MessageKey.CMD_DESC_COMPLETE; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        DPlayerProfiles plugin = DPlayerProfiles.getInstance();
        if (args.length < 3) {
            if (sender instanceof Player p) plugin.getLangManager().send(p, MessageKey.CMD_COMPLETE_USAGE);
            else sender.sendMessage("Usage: /dp complete <player> <achievement>");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            if (sender instanceof Player p) plugin.getLangManager().send(p, MessageKey.PLAYER_NOT_FOUND,
                    Placeholder.of("player", args[1]));
            else sender.sendMessage("Player not online: " + args[1]);
            return;
        }

        String achievementId = args[2];
        AchievementConfig ac = plugin.getAchievementConfigLoader().get(achievementId);
        if (ac == null) {
            if (sender instanceof Player p) plugin.getLangManager().send(p, MessageKey.CMD_COMPLETE_NOT_FOUND,
                    Placeholder.of("achievement", achievementId));
            else sender.sendMessage("Achievement not found: " + achievementId);
            return;
        }

        PlayerProfile profile = plugin.getProfileManager().get(target.getUniqueId());
        if (profile == null) {
            if (sender instanceof Player p) plugin.getLangManager().send(p, MessageKey.PLAYER_NOT_FOUND,
                    Placeholder.of("player", target.getName()));
            return;
        }

        if (profile.isCompleted(achievementId)) {
            if (sender instanceof Player p) plugin.getLangManager().send(p, MessageKey.CMD_COMPLETE_ALREADY,
                    Placeholder.of("player", target.getName()));
            else sender.sendMessage(target.getName() + " already has that achievement.");
            return;
        }

        plugin.getAchievementManager().forceComplete(target, profile, ac);
        if (sender instanceof Player p) {
            plugin.getLangManager().send(p, MessageKey.CMD_COMPLETE_SUCCESS,
                    Placeholder.of("player", target.getName()),
                    Placeholder.of("achievement", achievementId));
        } else {
            sender.sendMessage("Completed " + achievementId + " for " + target.getName());
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        DPlayerProfiles plugin = DPlayerProfiles.getInstance();
        if (args.length == 2) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName)
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
