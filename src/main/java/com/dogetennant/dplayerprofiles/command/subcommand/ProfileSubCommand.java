package com.dogetennant.dplayerprofiles.command.subcommand;

import com.dogetennant.dplayerprofiles.DPlayerProfiles;
import com.dogetennant.dplayerprofiles.command.SubCommand;
import com.dogetennant.dplayerprofiles.gui.DynamicGui;
import com.dogetennant.dplayerprofiles.lang.MessageKey;
import com.dogetennant.dplayerprofiles.lang.Placeholder;
import com.dogetennant.dplayerprofiles.model.gui.GuiLayout;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Collectors;

public class ProfileSubCommand implements SubCommand {

    @Override
    public String getName() { return "profile"; }

    @Override
    public String getPermission() { return "dplayerprofiles.use"; }

    @Override
    public MessageKey getDescriptionKey() { return MessageKey.CMD_DESC_PROFILE; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player viewer)) {
            sender.sendMessage("This command can only be run by a player.");
            return;
        }
        DPlayerProfiles plugin = DPlayerProfiles.getInstance();

        if (args.length < 2) {
            var profile = plugin.getProfileManager().get(viewer.getUniqueId());
            if (profile == null) {
                plugin.getLangManager().send(viewer, MessageKey.PLAYER_NOT_FOUND,
                        Placeholder.of("player", viewer.getName()));
                return;
            }
            GuiLayout layout = plugin.getGuiLayoutLoader().get("profile");
            if (layout == null) return;
            plugin.getGuiManager().open(viewer, new DynamicGui(plugin, layout, profile, null));
            return;
        }

        if (!viewer.hasPermission("dplayerprofiles.profile.others")) {
            plugin.getLangManager().send(viewer, MessageKey.NO_PERMISSION);
            return;
        }
        String targetName = args[1];
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
