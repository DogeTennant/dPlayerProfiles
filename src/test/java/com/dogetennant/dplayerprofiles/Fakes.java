package com.dogetennant.dplayerprofiles;

import com.dogetennant.dplayerprofiles.config.ConfigManager;
import com.dogetennant.dplayerprofiles.config.MainConfig;
import com.dogetennant.dplayerprofiles.database.DatabaseQueue;
import com.dogetennant.dplayerprofiles.lang.LanguageManager;
import com.dogetennant.dplayerprofiles.reward.AchievementRewardStorage;
import com.dogetennant.dplayerprofiles.util.LogUtil;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test doubles: a mocked plugin with a real {@link MainConfig}, a mocked Bukkit server whose
 * scheduler runs main-thread tasks at once and keeps background tasks for the test to run, and
 * firing an event at a listener the way the server does.
 */
public final class Fakes {

    private static Server server;

    /** Background tasks ({@code runTaskAsynchronously}) waiting for {@link #runBackground}. */
    public static final List<Runnable> BACKGROUND = new ArrayList<>();

    private Fakes() {
    }

    public static DPlayerProfiles plugin(MainConfig config) {
        DPlayerProfiles plugin = mock(DPlayerProfiles.class);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("dPlayerProfiles-test"));
        LogUtil.init(plugin);
        ConfigManager configManager = mock(ConfigManager.class);
        when(configManager.get()).thenReturn(config);
        when(plugin.getConfigManager()).thenReturn(configManager);
        when(plugin.getLangManager()).thenReturn(mock(LanguageManager.class));
        when(plugin.getAchievementRewardStorage()).thenReturn(mock(AchievementRewardStorage.class));
        when(plugin.getDatabaseQueue()).thenReturn(DatabaseQueue.immediate(Logger.getLogger("dPlayerProfiles-test")));
        Server bukkit = server();   // not inside when(...): stubbing while stubbing breaks Mockito
        when(plugin.getServer()).thenReturn(bukkit);
        when(plugin.getName()).thenReturn("dPlayerProfiles");
        when(plugin.namespace()).thenReturn("dplayerprofiles");
        BACKGROUND.clear();
        return plugin;
    }

    /** A config with the defaults the tests rely on: chat notifications, streaks on, no points. */
    public static MainConfig config() {
        MainConfig config = new MainConfig();
        config.achievementNotification = "chat";
        config.streaksEnabled = true;
        config.antiFarmEnabled = true;
        return config;
    }

    /** The mocked Bukkit server, installed once per test JVM (stub what a test needs). */
    public static synchronized Server server() {
        if (server == null) {
            server = mock(Server.class);
            when(server.getLogger()).thenReturn(Logger.getLogger("server-test"));
            BukkitScheduler scheduler = mock(BukkitScheduler.class);
            when(scheduler.runTask(any(Plugin.class), any(Runnable.class))).thenAnswer(call -> {
                call.<Runnable>getArgument(1).run();
                return null;
            });
            when(scheduler.runTaskAsynchronously(any(Plugin.class), any(Runnable.class))).thenAnswer(call -> {
                BACKGROUND.add(call.getArgument(1));
                return null;
            });
            when(scheduler.scheduleSyncRepeatingTask(any(Plugin.class), any(Runnable.class), anyLong(), anyLong()))
                    .thenReturn(1);
            when(server.getScheduler()).thenReturn(scheduler);
            try {
                Field field = Bukkit.class.getDeclaredField("server");
                field.setAccessible(true);
                field.set(null, server);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Cannot install the test server", e);
            }
        }
        return server;
    }

    /** Runs the waiting background tasks, newest first when {@code reversed} (threads give no order). */
    public static void runBackground(boolean reversed) {
        List<Runnable> tasks = new ArrayList<>(BACKGROUND);
        BACKGROUND.clear();
        if (reversed) Collections.reverse(tasks);
        tasks.forEach(Runnable::run);
    }

    /**
     * Calls every {@link EventHandler} of {@code listener} that takes this kind of event, skipping
     * {@code ignoreCancelled} handlers once the event is cancelled - like the server does.
     */
    public static void fire(Listener listener, Event event) {
        for (Method method : listener.getClass().getMethods()) {
            EventHandler handler = method.getAnnotation(EventHandler.class);
            if (handler == null || method.getParameterCount() != 1) continue;
            if (!method.getParameterTypes()[0].isAssignableFrom(event.getClass())) continue;
            if (handler.ignoreCancelled() && event instanceof Cancellable c && c.isCancelled()) continue;
            try {
                method.invoke(listener, event);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new IllegalStateException(e);
            }
        }
    }
}
