package com.dogetennant.dplayerprofiles.command.subcommand;

import com.dogetennant.dplayerprofiles.DPlayerProfiles;
import com.dogetennant.dplayerprofiles.command.SubCommand;
import com.dogetennant.dplayerprofiles.lang.MessageKey;
import com.dogetennant.dplayerprofiles.model.PlayerProfile;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/** /dp private [on|off] - toggle profile privacy. */
public class PrivacySubCommand implements SubCommand {

    @Override
    public String getName() { return "private"; }

    @Override
    public String getPermission() { return "dplayerprofiles.use"; }

    @Override
    public MessageKey getDescriptionKey() { return MessageKey.CMD_DESC_PRIVATE; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be run by a player.");
            return;
        }

        DPlayerProfiles plugin = DPlayerProfiles.getInstance();
        PlayerProfile profile = plugin.getProfileManager().get(player.getUniqueId());
        if (profile == null) return;

        boolean newState;
        if (args.length < 2) {
            newState = !profile.isPrivate(); // toggle
        } else {
            String arg = args[1].toLowerCase();
            if (arg.equals("on") || arg.equals("true") || arg.equals("enable")) {
                newState = true;
            } else if (arg.equals("off") || arg.equals("false") || arg.equals("disable")) {
                newState = false;
            } else {
                plugin.getLangManager().send(player, MessageKey.CMD_PRIVATE_USAGE);
                return;
            }
        }

        plugin.getProfileManager().setPrivacy(player.getUniqueId(), newState);
        plugin.getLangManager().send(player, newState
                ? MessageKey.PROFILE_PRIVACY_ENABLED
                : MessageKey.PROFILE_PRIVACY_DISABLED);
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) return List.of("on", "off");
        return List.of();
    }
}
