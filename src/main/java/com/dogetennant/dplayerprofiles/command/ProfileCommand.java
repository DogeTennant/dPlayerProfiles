package com.dogetennant.dplayerprofiles.command;

import com.dogetennant.dplayerprofiles.DPlayerProfiles;
import com.dogetennant.dplayerprofiles.gui.DynamicGui;
import com.dogetennant.dplayerprofiles.lang.MessageKey;
import com.dogetennant.dplayerprofiles.lang.Placeholder;
import com.dogetennant.dplayerprofiles.model.gui.GuiLayout;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Handles /profile [player] and /me as standalone commands.
 * /me always opens the sender's own profile (no argument accepted).
 */
public class ProfileCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                              @NotNull String label, String[] args) {
        if (!(sender instanceof Player viewer)) {
            sender.sendMessage("This command can only be run by a player.");
            return true;
        }

        DPlayerProfiles plugin = DPlayerProfiles.getInstance();
        boolean isMeAlias = label.equalsIgnoreCase("me");

        if (isMeAlias || args.length == 0) {
            var profile = plugin.getProfileManager().get(viewer.getUniqueId());
            if (profile == null) {
                plugin.getLangManager().send(viewer, MessageKey.PLAYER_NOT_FOUND,
                        Placeholder.of("player", viewer.getName()));
                return true;
            }
            GuiLayout layout = plugin.getGuiLayoutLoader().get("profile");
            if (layout == null) return true;
            plugin.getGuiManager().open(viewer, new DynamicGui(plugin, layout, profile, null));
            return true;
        }

        if (!viewer.hasPermission("dplayerprofiles.profile.others")) {
            plugin.getLangManager().send(viewer, MessageKey.NO_PERMISSION);
            return true;
        }
        String targetName = args[0];
        plugin.getProfileManager().loadProfileByNameAsync(targetName, profile -> {
            if (profile == null) {
                plugin.getLangManager().send(viewer, MessageKey.PLAYER_NOT_FOUND,
                        Placeholder.of("player", targetName));
                return;
            }
            if (profile.isPrivate() && !viewer.hasPermission("dplayerprofiles.profile.bypass-private")) {
                plugin.getLangManager().send(viewer, MessageKey.PROFILE_PRIVATE,
                        Placeholder.of("player", profile.getUsername()));
                return;
            }
            GuiLayout layout = plugin.getGuiLayoutLoader().get("profile");
            if (layout == null) return;
            plugin.getGuiManager().open(viewer, new DynamicGui(plugin, layout, profile, null));
        });
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                       @NotNull String label, String[] args) {
        if (label.equalsIgnoreCase("me") || !(sender instanceof Player)) return List.of();
        if (args.length == 1) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return List.of();
    }
}
