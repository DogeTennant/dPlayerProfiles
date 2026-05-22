package com.dogetennant.dplayerprofiles.command.subcommand;

import com.dogetennant.dplayerprofiles.DPlayerProfiles;
import com.dogetennant.dplayerprofiles.command.SubCommand;
import com.dogetennant.dplayerprofiles.lang.MessageKey;
import com.dogetennant.dplayerprofiles.lang.Placeholder;
import com.dogetennant.dplayerprofiles.util.ColorUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class HelpSubCommand implements SubCommand {

    @Override
    public String getName() { return "help"; }

    @Override
    public String getPermission() { return "dplayerprofiles.use"; }

    @Override
    public MessageKey getDescriptionKey() { return MessageKey.CMD_DESC_HELP; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        DPlayerProfiles plugin = DPlayerProfiles.getInstance();
        StringBuilder sb = new StringBuilder();
        sb.append(plugin.getLangManager().getRaw(MessageKey.CMD_HELP_HEADER)).append("\n");

        for (SubCommand sub : plugin.getCommandRegistry().getAll()) {
            if (!sender.hasPermission(sub.getPermission())) continue;
            MessageKey descKey = sub.getDescriptionKey();
            String desc = descKey != null ? plugin.getLangManager().getRaw(descKey) : "";
            sb.append(plugin.getLangManager().getRaw(MessageKey.CMD_HELP_ENTRY,
                    Placeholder.of("name", sub.getName()),
                    Placeholder.of("description", desc))).append("\n");
        }

        String raw = sb.toString().stripTrailing();
        if (sender instanceof Player p) {
            p.sendMessage(ColorUtil.parse(raw));
        } else {
            sender.sendMessage(raw);
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) { return List.of(); }
}
