package com.dogetennant.dplayerprofiles.util;

import org.bukkit.entity.Player;
import org.bukkit.metadata.MetadataValue;

/**
 * Vanish detection through the "vanished" metadata every common vanish plugin sets (dVanish,
 * PremiumVanish, SuperVanish, Essentials, CMI, ...) - no dependency on any of them.
 */
public final class VanishUtil {

    private VanishUtil() {}

    public static boolean isVanished(Player player) {
        for (MetadataValue value : player.getMetadata("vanished")) {
            if (value.asBoolean()) return true;
        }
        return false;
    }
}
