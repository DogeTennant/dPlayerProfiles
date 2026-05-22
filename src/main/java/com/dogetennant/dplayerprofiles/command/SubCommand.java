package com.dogetennant.dplayerprofiles.command;

import com.dogetennant.dplayerprofiles.lang.MessageKey;
import org.bukkit.command.CommandSender;

import java.util.List;

public interface SubCommand {

    String getName();

    default List<String> getAliases() { return List.of(); }

    String getPermission();

    default MessageKey getDescriptionKey() { return null; }

    void execute(CommandSender sender, String[] args);

    List<String> tabComplete(CommandSender sender, String[] args);
}
