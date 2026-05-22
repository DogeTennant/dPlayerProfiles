package com.dogetennant.dplayerprofiles.command;

import com.dogetennant.dplayerprofiles.DPlayerProfiles;
import com.dogetennant.dplayerprofiles.gui.DynamicGui;
import com.dogetennant.dplayerprofiles.lang.MessageKey;
import com.dogetennant.dplayerprofiles.model.PlayerProfile;
import com.dogetennant.dplayerprofiles.model.gui.GuiLayout;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class DPlayerProfilesCommand implements CommandExecutor, TabCompleter {

    private final CommandRegistry registry;

    public DPlayerProfilesCommand(CommandRegistry registry) {
        this.registry = registry;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                              @NotNull String label, String[] args) {
        DPlayerProfiles plugin = DPlayerProfiles.getInstance();

        if (args.length == 0) {
            if (sender instanceof Player player) {
                PlayerProfile profile = plugin.getProfileManager().get(player.getUniqueId());
                if (profile != null) {
                    GuiLayout layout = plugin.getGuiLayoutLoader().get("profile");
                    if (layout != null) {
                        plugin.getGuiManager().open(player, new DynamicGui(plugin, layout, profile, null));
                    }
                } else {
                    plugin.getLangManager().send(player, MessageKey.PLAYER_NOT_FOUND,
                            com.dogetennant.dplayerprofiles.lang.Placeholder.of("player", player.getName()));
                }
            } else {
                registry.get("help").ifPresent(h -> h.execute(sender, args));
            }
            return true;
        }

        Optional<SubCommand> subOpt = registry.get(args[0]);
        if (subOpt.isEmpty()) {
            if (sender instanceof Player p) {
                plugin.getLangManager().send(p, MessageKey.UNKNOWN_COMMAND);
            } else {
                sender.sendMessage("Unknown subcommand: " + args[0]);
            }
            return true;
        }

        SubCommand sub = subOpt.get();
        if (!sender.hasPermission(sub.getPermission())) {
            if (sender instanceof Player p) {
                plugin.getLangManager().send(p, MessageKey.NO_PERMISSION);
            } else {
                sender.sendMessage("No permission.");
            }
            return true;
        }

        sub.execute(sender, args);
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                       @NotNull String label, String[] args) {
        if (args.length == 1) {
            return registry.getAll().stream()
                    .filter(sub -> sender.hasPermission(sub.getPermission()))
                    .map(SubCommand::getName)
                    .filter(name -> name.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length >= 2) {
            return registry.get(args[0])
                    .filter(sub -> sender.hasPermission(sub.getPermission()))
                    .map(sub -> sub.tabComplete(sender, args))
                    .orElse(List.of());
        }
        return List.of();
    }
}
