package com.dogetennant.dplayerprofiles.command;

import com.dogetennant.dplayerprofiles.DPlayerProfiles;
import com.dogetennant.dplayerprofiles.lang.MessageKey;
import com.dogetennant.dplayerprofiles.lang.Placeholder;
import com.dogetennant.dplayerprofiles.model.PlayerProfile;
import com.dogetennant.dplayerprofiles.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class CommandUtil {

    private CommandUtil() {}

    /**
     * Whether a private profile must not be shown to this viewer - another player's, without the
     * bypass permission - and tells them so. The one check for every command that shows someone
     * else's profile or achievements.
     */
    public static boolean refusedAsPrivate(Player viewer, PlayerProfile profile) {
        if (!profile.isPrivate() || viewer.getUniqueId().equals(profile.getUuid())
                || viewer.hasPermission("dplayerprofiles.profile.bypass-private")) {
            return false;
        }
        DPlayerProfiles.getInstance().getLangManager().send(viewer, MessageKey.PROFILE_PRIVATE,
                Placeholder.of("player", profile.getUsername()));
        return true;
    }

    /**
     * Sends a language message to any sender (player or console), from any thread.
     * Console output is delivered on the main thread like everything else.
     */
    public static void reply(CommandSender sender, MessageKey key, Placeholder... placeholders) {
        DPlayerProfiles plugin = DPlayerProfiles.getInstance();
        Runnable send = () -> sender.sendMessage(ColorUtil.parse(plugin.getLangManager().getRaw(key, placeholders)));
        if (Bukkit.isPrimaryThread()) {
            send.run();
        } else {
            Bukkit.getScheduler().runTask(plugin, send);
        }
    }
}
