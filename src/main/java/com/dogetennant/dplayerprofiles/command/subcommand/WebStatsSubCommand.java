package com.dogetennant.dplayerprofiles.command.subcommand;

import com.dogetennant.dplayerprofiles.DPlayerProfiles;
import com.dogetennant.dplayerprofiles.command.CommandUtil;
import com.dogetennant.dplayerprofiles.command.SubCommand;
import com.dogetennant.dplayerprofiles.lang.MessageKey;
import com.dogetennant.dplayerprofiles.lang.Placeholder;
import com.dogetennant.dplayerprofiles.stats.WebStatsExporter;
import org.bukkit.command.CommandSender;

import java.util.List;

/** /dp webstats backfill - re-imports all offline players and refreshes every balance. */
public class WebStatsSubCommand implements SubCommand {

    @Override
    public String getName() { return "webstats"; }

    @Override
    public String getPermission() { return "dplayerprofiles.admin"; }

    @Override
    public MessageKey getDescriptionKey() { return MessageKey.CMD_DESC_WEBSTATS; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 2 || !args[1].equalsIgnoreCase("backfill")) {
            CommandUtil.reply(sender, MessageKey.CMD_WEBSTATS_USAGE);
            return;
        }

        WebStatsExporter exporter = DPlayerProfiles.getInstance().getWebStatsExporter();
        if (!exporter.isEnabled()) {
            CommandUtil.reply(sender, MessageKey.CMD_WEBSTATS_DISABLED);
            return;
        }

        boolean started = exporter.backfill(true, result ->
                CommandUtil.reply(sender, MessageKey.CMD_WEBSTATS_BACKFILL_DONE,
                        Placeholder.of("players", String.valueOf(result.playersImported())),
                        Placeholder.of("balances", String.valueOf(result.balancesUpdated()))));
        CommandUtil.reply(sender, started
                ? MessageKey.CMD_WEBSTATS_BACKFILL_STARTED
                : MessageKey.CMD_WEBSTATS_ALREADY_RUNNING);
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 2 && "backfill".startsWith(args[1].toLowerCase())) return List.of("backfill");
        return List.of();
    }
}
