package com.dogetennant.dplayerprofiles.command.subcommand;

import com.dogetennant.dplayerprofiles.DPlayerProfiles;
import com.dogetennant.dplayerprofiles.command.SubCommand;
import com.dogetennant.dplayerprofiles.lang.MessageKey;
import com.dogetennant.dplayerprofiles.lang.Placeholder;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class LanguageSubCommand implements SubCommand {

    @Override
    public String getName() { return "language"; }

    @Override
    public String getPermission() { return "dplayerprofiles.admin"; }

    @Override
    public MessageKey getDescriptionKey() { return MessageKey.CMD_DESC_LANGUAGE; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        DPlayerProfiles plugin = DPlayerProfiles.getInstance();
        if (args.length < 2) {
            if (sender instanceof Player p) {
                plugin.getLangManager().send(p, MessageKey.CMD_LANGUAGE_USAGE);
            } else {
                sender.sendMessage("Usage: /dp language <code>");
            }
            return;
        }

        String code = args[1].toLowerCase();
        boolean success = plugin.getLangManager().switchLanguage(code);
        if (success) {
            // only the language line of config.yml, as it is on disk (hand edits and comments stay)
            boolean saved = plugin.getConfigManager().set("language", code);
            if (sender instanceof Player p) {
                plugin.getLangManager().send(p, MessageKey.CMD_LANGUAGE_SUCCESS,
                        Placeholder.of("code", code));
                if (!saved) plugin.getLangManager().send(p, MessageKey.FILE_BROKEN, Placeholder.of("file", "config.yml"));
            } else {
                sender.sendMessage("Language switched to " + code);
                if (!saved) sender.sendMessage("config.yml has a mistake (YAML error), so it was not changed.");
            }
        } else {
            if (sender instanceof Player p) {
                plugin.getLangManager().send(p, MessageKey.CMD_LANGUAGE_NOT_FOUND,
                        Placeholder.of("code", code));
            } else {
                sender.sendMessage("Language file not found: " + code);
            }
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) return List.of("en_us", "cs_cz");
        return List.of();
    }
}
