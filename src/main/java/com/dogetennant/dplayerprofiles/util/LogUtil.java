package com.dogetennant.dplayerprofiles.util;

import org.bukkit.plugin.Plugin;

import java.util.logging.Level;
import java.util.logging.Logger;

public final class LogUtil {

    private static Logger logger;

    private LogUtil() {}

    public static void init(Plugin plugin) {
        logger = plugin.getLogger();
    }

    public static void info(String message) {
        logger.info(message);
    }

    public static void warn(String message) {
        logger.warning(message);
    }

    public static void severe(String message) {
        logger.severe(message);
    }

    public static void severe(String message, Throwable t) {
        logger.log(Level.SEVERE, message, t);
    }

    public static void debug(String message) {
        logger.fine(message);
    }
}
