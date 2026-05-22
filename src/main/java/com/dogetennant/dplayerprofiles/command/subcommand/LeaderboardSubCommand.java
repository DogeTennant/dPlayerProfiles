package com.dogetennant.dplayerprofiles.command.subcommand;

import com.dogetennant.dplayerprofiles.DPlayerProfiles;
import com.dogetennant.dplayerprofiles.command.SubCommand;
import com.dogetennant.dplayerprofiles.gui.DynamicGui;
import com.dogetennant.dplayerprofiles.lang.MessageKey;
import com.dogetennant.dplayerprofiles.model.gui.GuiLayout;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class LeaderboardSubCommand implements SubCommand {

    @Override
    public String getName() { return "leaderboard"; }

    @Override
    public String getPermission() { return "dplayerprofiles.use"; }

    @Override
    public MessageKey getDescriptionKey() { return MessageKey.CMD_DESC_LEADERBOARD; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be run by a player.");
            return;
        }
        DPlayerProfiles plugin = DPlayerProfiles.getInstance();
        GuiLayout layout = plugin.getGuiLayoutLoader().get("leaderboard");
        if (layout == null) return;
        plugin.getGuiManager().open(player, new DynamicGui(plugin, layout, null, null));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) { return List.of(); }
}
