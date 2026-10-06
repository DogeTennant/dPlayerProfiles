package com.dogetennant.dplayerprofiles.command;

import com.dogetennant.dplayerprofiles.DPlayerProfiles;
import com.dogetennant.dplayerprofiles.lang.MessageKey;
import com.dogetennant.dplayerprofiles.lang.Placeholder;
import com.dogetennant.dplayerprofiles.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

public final class CommandUtil {

    private CommandUtil() {}

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
