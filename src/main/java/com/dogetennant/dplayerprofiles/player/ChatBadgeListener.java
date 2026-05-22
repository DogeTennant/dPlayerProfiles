package com.dogetennant.dplayerprofiles.player;

import com.dogetennant.dplayerprofiles.DPlayerProfiles;
import com.dogetennant.dplayerprofiles.lang.MessageKey;
import com.dogetennant.dplayerprofiles.model.BadgeConfig;
import com.dogetennant.dplayerprofiles.model.PlayerProfile;
import com.dogetennant.dplayerprofiles.util.ColorUtil;
import io.papermc.paper.chat.ChatRenderer;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.List;

/**
 * Adds a hover tooltip to the player's name in chat listing their badges.
 */
public class ChatBadgeListener implements Listener {

    private final DPlayerProfiles plugin;

    public ChatBadgeListener(DPlayerProfiles plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        PlayerProfile profile = plugin.getProfileManager().get(event.getPlayer().getUniqueId());
        if (profile == null || profile.getBadges().isEmpty()) return;
        List<String> chatBadges = profile.getEffectiveChatBadges();
        if (chatBadges.isEmpty()) return;

        Component tooltip = buildTooltip(chatBadges);
        ChatRenderer original = event.renderer();
        event.renderer((source, sourceDisplayName, message, viewer) -> {
            Component nameWithHover = sourceDisplayName.hoverEvent(HoverEvent.showText(tooltip));
            return original.render(source, nameWithHover, message, viewer);
        });
    }

    private Component buildTooltip(List<String> badgeIds) {
        List<Component> lines = new ArrayList<>();
        lines.add(ColorUtil.parse(plugin.getLangManager().getRaw(MessageKey.CHAT_BADGE_HEADER)));
        String entryTemplate = plugin.getLangManager().getRaw(MessageKey.CHAT_BADGE_ENTRY);
        for (String badgeId : badgeIds) {
            BadgeConfig bc = plugin.getBadgeConfigLoader().get(badgeId);
            if (bc != null) {
                lines.add(ColorUtil.parse(entryTemplate.replace("{badge}", bc.displayName)));
            }
        }
        Component result = lines.get(0);
        for (int i = 1; i < lines.size(); i++) {
            result = result.append(Component.newline()).append(lines.get(i));
        }
        return result;
    }
}
