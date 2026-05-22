package com.dogetennant.dplayerprofiles.command.subcommand;

import com.dogetennant.dplayerprofiles.DPlayerProfiles;
import com.dogetennant.dplayerprofiles.command.SubCommand;
import com.dogetennant.dplayerprofiles.lang.MessageKey;
import com.dogetennant.dplayerprofiles.lang.Placeholder;
import com.dogetennant.dplayerprofiles.model.BadgeConfig;
import com.dogetennant.dplayerprofiles.model.PlayerProfile;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Collectors;

public class BadgeSubCommand implements SubCommand {

    @Override
    public String getName() { return "badge"; }

    @Override
    public String getPermission() { return "dplayerprofiles.admin"; }

    @Override
    public MessageKey getDescriptionKey() { return MessageKey.CMD_DESC_BADGE; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        DPlayerProfiles plugin = DPlayerProfiles.getInstance();
        // /dp badge give|remove <player> <badge>
        if (args.length < 4) {
            if (sender instanceof Player p) plugin.getLangManager().send(p, MessageKey.CMD_BADGE_USAGE);
            else sender.sendMessage("Usage: /dp badge give|remove <player> <badge>");
            return;
        }

        String action = args[1].toLowerCase();
        if (!action.equals("give") && !action.equals("remove")) {
            if (sender instanceof Player p) plugin.getLangManager().send(p, MessageKey.CMD_BADGE_USAGE);
            else sender.sendMessage("Usage: /dp badge give|remove <player> <badge>");
            return;
        }

        Player target = Bukkit.getPlayer(args[2]);
        if (target == null) {
            if (sender instanceof Player p) plugin.getLangManager().send(p, MessageKey.PLAYER_NOT_FOUND,
                    Placeholder.of("player", args[2]));
            else sender.sendMessage("Player not online: " + args[2]);
            return;
        }

        String badgeId = args[3].toLowerCase();
        BadgeConfig bc = plugin.getBadgeConfigLoader().get(badgeId);
        if (bc == null) {
            if (sender instanceof Player p) plugin.getLangManager().send(p, MessageKey.CMD_BADGE_NOT_FOUND,
                    Placeholder.of("badge", badgeId));
            else sender.sendMessage("Badge not found: " + badgeId);
            return;
        }

        PlayerProfile profile = plugin.getProfileManager().get(target.getUniqueId());
        if (profile == null) {
            if (sender instanceof Player p) plugin.getLangManager().send(p, MessageKey.PLAYER_NOT_FOUND,
                    Placeholder.of("player", target.getName()));
            return;
        }

        if (action.equals("give")) {
            if (profile.hasBadge(badgeId)) {
                if (sender instanceof Player p) plugin.getLangManager().send(p, MessageKey.CMD_BADGE_ALREADY_HAS,
                        Placeholder.of("player", target.getName()));
                else sender.sendMessage(target.getName() + " already has that badge.");
                return;
            }
            long now = System.currentTimeMillis();
            String grantedBy = sender instanceof Player p ? p.getUniqueId().toString() : null;
            profile.grantBadge(badgeId, now);
            plugin.getProfileManager().saveBadge(target.getUniqueId(), badgeId, now, grantedBy);
            plugin.getAchievementManager().checkChainAchievements(target, profile);
            if (sender instanceof Player p) plugin.getLangManager().send(p, MessageKey.CMD_BADGE_GIVE_SUCCESS,
                    Placeholder.of("player", target.getName()), Placeholder.of("badge", badgeId));
            else sender.sendMessage("Gave badge " + badgeId + " to " + target.getName());
        } else {
            if (!profile.hasBadge(badgeId)) {
                if (sender instanceof Player p) plugin.getLangManager().send(p, MessageKey.CMD_BADGE_DOESNT_HAVE,
                        Placeholder.of("player", target.getName()));
                else sender.sendMessage(target.getName() + " does not have that badge.");
                return;
            }
            profile.revokeBadge(badgeId);
            plugin.getProfileManager().removeBadge(target.getUniqueId(), badgeId);
            if (sender instanceof Player p) plugin.getLangManager().send(p, MessageKey.CMD_BADGE_REMOVE_SUCCESS,
                    Placeholder.of("player", target.getName()), Placeholder.of("badge", badgeId));
            else sender.sendMessage("Removed badge " + badgeId + " from " + target.getName());
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        DPlayerProfiles plugin = DPlayerProfiles.getInstance();
        if (args.length == 2) return List.of("give", "remove");
        if (args.length == 3) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 4) {
            return plugin.getBadgeConfigLoader().getAll().keySet().stream()
                    .filter(id -> id.startsWith(args[3].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return List.of();
    }
}
